#!/usr/bin/env python3
"""
Seed and export script for LEARN-Hub activities.

Subcommands:
    seed    Upload PDFs and create activities from the dataset CSV.
    export  Fetch generated markdowns from the server and write them back into the CSV.

Usage:
    python seed.py seed [options]
    python seed.py export [options]

See --help on each subcommand for details.

Environment variables (override defaults):
    SEED_BASE_URL       Server base URL        (default: http://localhost:5001)
    SEED_ADMIN_EMAIL    Admin email            (default: admin@learnhub.com)
    SEED_ADMIN_PASSWORD Admin password         (required for seed)
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
import time
from pathlib import Path

try:
    import requests
except ImportError:
    print("Error: 'requests' package is required. Install it with: pip install requests")
    sys.exit(1)

SCRIPT_DIR = Path(__file__).resolve().parent
CSV_PATH = SCRIPT_DIR / "dataset.csv"
PDF_DIR = SCRIPT_DIR / "pdfs"

METADATA_FIELDS = [
    "name", "source", "description", "ageMin", "ageMax", "format",
    "bloomLevel", "durationMinMinutes", "durationMaxMinutes",
    "mentalLoad", "physicalEnergy", "prepTimeMinutes", "cleanupTimeMinutes",
]

INT_FIELDS = {
    "ageMin", "ageMax",
    "durationMinMinutes", "durationMaxMinutes",
    "prepTimeMinutes", "cleanupTimeMinutes",
}

LIST_FIELDS = ["resourcesNeeded", "topics"]

MARKDOWN_FIELDS = [
    "deckblattMarkdown",
    "artikulationsschemaMarkdown",
    "hintergrundwissenMarkdown",
]

MARKDOWN_API_TYPES = {
    "deckblatt": "deckblattMarkdown",
    "artikulationsschema": "artikulationsschemaMarkdown",
    "hintergrundwissen": "hintergrundwissenMarkdown",
}


# ---------------------------------------------------------------------------
# Shared helpers
# ---------------------------------------------------------------------------

def read_csv() -> tuple[list[str], list[dict]]:
    """Read the dataset CSV. Returns (fieldnames, rows)."""
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        return list(reader.fieldnames), list(reader)


def fetch_activities(base_url: str, timeout: int) -> list[dict]:
    """Fetch all activities with their markdowns from the API (no auth required)."""
    resp = requests.get(
        f"{base_url}/api/activities/",
        params={"limit": 1000, "offset": 0},
        timeout=timeout,
    )
    if resp.status_code != 200:
        print(f"Error fetching activities ({resp.status_code}): {resp.text}")
        sys.exit(1)
    return resp.json().get("activities", [])


def get_existing_activity_names(base_url: str, timeout: int) -> set[str]:
    """Fetch all existing activity names (no auth required for listing)."""
    resp = requests.get(
        f"{base_url}/api/activities/",
        params={"limit": 1000, "offset": 0},
        timeout=timeout,
    )
    if resp.status_code != 200:
        return set()
    activities = resp.json().get("activities", [])
    return {a["name"] for a in activities if "name" in a}


def login(base_url: str, email: str, password: str, timeout: int) -> str:
    """Authenticate as admin and return the access token."""
    resp = requests.post(
        f"{base_url}/api/auth/admin/login",
        json={"email": email, "password": password},
        timeout=timeout,
    )
    if resp.status_code != 200:
        print(f"Login failed ({resp.status_code}): {resp.text}")
        sys.exit(1)

    token = resp.json().get("accessToken")
    if not token:
        print(f"Login response missing accessToken: {resp.json()}")
        sys.exit(1)

    return token


# ---------------------------------------------------------------------------
# Seed helpers
# ---------------------------------------------------------------------------

def csv_row_to_metadata(row: dict) -> dict:
    """Extract metadata fields from a CSV row."""
    metadata = {}
    for key in METADATA_FIELDS:
        val = row.get(key, "").strip()
        if not val:
            continue
        if key in INT_FIELDS:
            metadata[key] = int(val)
        else:
            metadata[key] = val

    for key in LIST_FIELDS:
        val = row.get(key, "").strip()
        if val:
            metadata[key] = [item.strip() for item in val.split("|") if item.strip()]

    return metadata


def csv_row_to_markdowns(row: dict) -> dict | None:
    """Extract markdown content from CSV columns. Returns None if any field is missing/empty."""
    markdowns = {}
    for key in MARKDOWN_FIELDS:
        val = row.get(key, "").strip()
        if not val:
            return None
        markdowns[key] = val
    return markdowns


def upload_pdf(base_url: str, token: str, pdf_path: Path, timeout: int) -> dict:
    """Upload a PDF. Returns the API response dict."""
    with open(pdf_path, "rb") as f:
        resp = requests.post(
            f"{base_url}/api/activities/upload-pdf-draft",
            files={"pdf_file": (pdf_path.name, f, "application/pdf")},
            params={"extractMetadata": "false"},
            headers={"Authorization": f"Bearer {token}"},
            timeout=timeout,
        )
    if resp.status_code != 201:
        raise RuntimeError(f"Upload failed ({resp.status_code}): {resp.text}")
    return resp.json()


def generate_markdowns_api(
    base_url: str, token: str, document_id: str, metadata: dict, timeout: int
) -> dict:
    """Generate all three markdowns via the LLM. Returns the API response dict."""
    resp = requests.post(
        f"{base_url}/api/activities/generate-activity-markdowns",
        json={"documentId": document_id, "metadata": metadata},
        headers={"Authorization": f"Bearer {token}"},
        timeout=timeout,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"Markdown generation failed ({resp.status_code}): {resp.text}")
    return resp.json()


def create_activity(
    base_url: str, token: str, document_id: str, metadata: dict, markdowns: dict, timeout: int
) -> dict:
    """Create the final activity with metadata and markdowns."""
    payload = {
        "documentId": document_id,
        **metadata,
        "deckblattMarkdown": markdowns.get("deckblattMarkdown", ""),
        "artikulationsschemaMarkdown": markdowns.get("artikulationsschemaMarkdown", ""),
        "hintergrundwissenMarkdown": markdowns.get("hintergrundwissenMarkdown", ""),
    }
    resp = requests.post(
        f"{base_url}/api/activities/create",
        json=payload,
        headers={"Authorization": f"Bearer {token}"},
        timeout=timeout,
    )
    if resp.status_code != 201:
        raise RuntimeError(f"Activity creation failed ({resp.status_code}): {resp.text}")
    return resp.json()


def seed_activity(
    base_url: str, token: str, row: dict, timeout: int, generate_markdown: bool = False
) -> str:
    """Run the full seed pipeline for one CSV row. Returns the created activity ID."""
    filename = row["filename"]
    pdf_path = PDF_DIR / filename
    metadata = csv_row_to_metadata(row)

    print(f"  [1/3] Uploading {filename}...")
    upload_result = upload_pdf(base_url, token, pdf_path, timeout)
    document_id = upload_result["documentId"]

    csv_markdowns = None if generate_markdown else csv_row_to_markdowns(row)

    if csv_markdowns:
        print(f"  [2/3] Using markdowns from CSV")
        markdowns = csv_markdowns
    else:
        if not generate_markdown:
            print(f"  [2/3] CSV markdowns missing/incomplete, falling back to LLM generation...")
        else:
            print(f"  [2/3] Generating markdowns via API (this may take a while)...")
        start = time.time()
        markdowns = generate_markdowns_api(base_url, token, document_id, metadata, timeout)
        elapsed = time.time() - start
        print(f"        Done in {elapsed:.1f}s")

    print(f"  [3/3] Creating activity...")
    result = create_activity(base_url, token, document_id, metadata, markdowns, timeout)
    return result.get("activity", {}).get("id", "unknown")


# ---------------------------------------------------------------------------
# Export helpers
# ---------------------------------------------------------------------------

def build_markdown_lookup(activities: list[dict]) -> dict[str, dict[str, str]]:
    """Build a lookup: activity name -> {deckblattMarkdown, ...}."""
    lookup = {}
    for activity in activities:
        name = activity.get("name", "")
        markdowns = {}
        for md in activity.get("markdowns", []):
            md_type = md.get("type", "").lower()
            csv_col = MARKDOWN_API_TYPES.get(md_type)
            if csv_col:
                markdowns[csv_col] = md.get("content", "")
        lookup[name] = markdowns
    return lookup


# ---------------------------------------------------------------------------
# Subcommand: seed
# ---------------------------------------------------------------------------

def cmd_seed(args):
    if not args.password:
        print("Error: Admin password is required. Use --password or set SEED_ADMIN_PASSWORD.")
        sys.exit(1)

    if not CSV_PATH.exists():
        print(f"Error: CSV file not found at {CSV_PATH}")
        sys.exit(1)

    if not PDF_DIR.exists():
        print(f"Error: PDF directory not found at {PDF_DIR}")
        sys.exit(1)

    _, rows = read_csv()
    print(f"Loaded {len(rows)} activities from {CSV_PATH.name}")

    if args.only:
        only_set = set(args.only)
        rows = [r for r in rows if r["filename"] in only_set]
        print(f"Filtered to {len(rows)} activities: {', '.join(r['filename'] for r in rows)}")

    if not rows:
        print("Nothing to seed.")
        return

    missing = [r["filename"] for r in rows if not (PDF_DIR / r["filename"]).exists()]
    if missing:
        print(f"Warning: {len(missing)} PDF files not found, will be skipped:")
        for m in missing:
            print(f"  - {m}")
        rows = [r for r in rows if r["filename"] not in set(missing)]

    print(f"\nLogging in as {args.email} at {args.base_url}...")
    token = login(args.base_url, args.email, args.password, args.timeout)
    print("Authenticated successfully.\n")

    existing_names = set()
    if args.skip_existing:
        existing_names = get_existing_activity_names(args.base_url, args.timeout)
        if existing_names:
            print(f"Found {len(existing_names)} existing activities on server.")

    succeeded = 0
    failed = 0
    skipped = 0

    for i, row in enumerate(rows, 1):
        name = row.get("name", row["filename"])
        print(f"[{i}/{len(rows)}] {name}")

        if args.skip_existing and name in existing_names:
            print(f"  Skipped (already exists)\n")
            skipped += 1
            continue

        try:
            activity_id = seed_activity(
                args.base_url, token, row, args.timeout, args.generate_markdown
            )
            print(f"  Created: {activity_id}\n")
            succeeded += 1
        except Exception as e:
            print(f"  FAILED: {e}\n")
            failed += 1

    print("=" * 60)
    print(f"Seeding complete: {succeeded} created, {skipped} skipped, {failed} failed")
    print("=" * 60)

    if failed > 0:
        sys.exit(1)


# ---------------------------------------------------------------------------
# Subcommand: export
# ---------------------------------------------------------------------------

def cmd_export(args):
    if not CSV_PATH.exists():
        print(f"Error: CSV file not found at {CSV_PATH}")
        sys.exit(1)

    fieldnames, rows = read_csv()
    print(f"Loaded {len(rows)} rows from {CSV_PATH.name}")

    print(f"Fetching activities from {args.base_url}...")
    activities = fetch_activities(args.base_url, args.timeout)
    print(f"Fetched {len(activities)} activities from server")

    lookup = build_markdown_lookup(activities)

    md_columns = list(MARKDOWN_API_TYPES.values())
    extended_fields = list(fieldnames)
    for col in md_columns:
        if col not in extended_fields:
            extended_fields.append(col)

    matched = 0
    for row in rows:
        name = row.get("name", "")
        md_data = lookup.get(name, {})
        if md_data:
            matched += 1
        for col in md_columns:
            row[col] = md_data.get(col, "")

    print(f"Matched {matched}/{len(rows)} activities by name")

    output_path = Path(args.output) if args.output else CSV_PATH
    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=extended_fields)
        writer.writeheader()
        writer.writerows(rows)

    print(f"Written extended CSV to {output_path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def main():
    parser = argparse.ArgumentParser(
        description="LEARN-Hub dataset tooling: seed activities and export markdowns"
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("SEED_BASE_URL", "http://localhost:5001"),
        help="Server base URL (default: $SEED_BASE_URL or http://localhost:5001)",
    )
    parser.add_argument(
        "--timeout",
        type=int,
        default=300,
        help="HTTP request timeout in seconds (default: 300)",
    )

    subparsers = parser.add_subparsers(dest="command", required=True)

    # -- seed --
    seed_parser = subparsers.add_parser("seed", help="Upload PDFs and create activities")
    seed_parser.add_argument(
        "--email",
        default=os.environ.get("SEED_ADMIN_EMAIL", "admin@learnhub.com"),
        help="Admin email (default: $SEED_ADMIN_EMAIL or admin@learnhub.com)",
    )
    seed_parser.add_argument(
        "--password",
        default=os.environ.get("SEED_ADMIN_PASSWORD"),
        help="Admin password (default: $SEED_ADMIN_PASSWORD)",
    )
    seed_parser.add_argument(
        "--only",
        nargs="+",
        metavar="FILENAME",
        help="Only seed these PDF filenames",
    )
    seed_parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Skip activities whose name already exists on the server",
    )
    seed_parser.add_argument(
        "--generate-markdown",
        action="store_true",
        help="Generate markdowns via LLM API instead of using CSV content (slower)",
    )

    # -- export --
    export_parser = subparsers.add_parser(
        "export", help="Fetch markdowns from server and write them into the CSV"
    )
    export_parser.add_argument(
        "-o", "--output",
        default=None,
        help="Output CSV path (default: overwrite dataset.csv in-place)",
    )

    args = parser.parse_args()

    if args.command == "seed":
        cmd_seed(args)
    elif args.command == "export":
        cmd_export(args)


if __name__ == "__main__":
    main()
