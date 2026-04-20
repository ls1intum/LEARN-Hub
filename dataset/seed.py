#!/usr/bin/env python3
"""
Seed and export script for LEARN-Hub activities.

Subcommands:
    seed    Upload PDFs and create activities from the dataset CSV.
    export  Fetch generated markdowns from the server and write them back into the CSV.
    delete  Delete selected activities from the server.
    delete-all
            Delete all activities from the server.

Usage:
    python seed.py seed [options]
    python seed.py export [options]
    python seed.py delete [options]
    python seed.py delete-all [options]

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
    requests = None

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


def require_requests():
    """Fail clearly when a network command is run without the requests package."""
    if requests is None:
        print("Error: 'requests' package is required. Install it with: pip install requests")
        sys.exit(1)


def auth_headers(token: str) -> dict[str, str]:
    """Build authorization headers for admin endpoints."""
    return {"Authorization": f"Bearer {token}"}


def fetch_activities(base_url: str, timeout: int, token: str | None = None) -> list[dict]:
    """Fetch all activities with their markdowns from the API."""
    headers = auth_headers(token) if token else None
    resp = requests.get(
        f"{base_url}/api/activities/",
        params={"limit": 1000, "offset": 0},
        headers=headers,
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
        f"{base_url}/api/auth/login",
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


def confirm_or_exit(message: str, assume_yes: bool = False):
    """Require an explicit confirmation for destructive operations."""
    if assume_yes:
        return

    answer = input(f"{message}\nType 'yes' to continue: ").strip().lower()
    if answer != "yes":
        print("Aborted.")
        sys.exit(1)


def delete_activity(base_url: str, token: str, activity_id: str, timeout: int):
    """Delete one activity by ID."""
    resp = requests.delete(
        f"{base_url}/api/activities/{activity_id}",
        headers=auth_headers(token),
        timeout=timeout,
    )
    if resp.status_code not in (200, 204):
        raise RuntimeError(f"Delete failed ({resp.status_code}): {resp.text}")


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
# Subcommands: delete / delete-all
# ---------------------------------------------------------------------------

def csv_filenames_to_names(filenames: list[str]) -> tuple[list[str], list[str]]:
    """Resolve CSV PDF filenames to activity names. Returns (names, missing filenames)."""
    if not CSV_PATH.exists():
        print(f"Error: CSV file not found at {CSV_PATH}")
        sys.exit(1)

    _, rows = read_csv()
    names_by_filename = {row.get("filename", ""): row.get("name", "") for row in rows}

    names = []
    missing = []
    for filename in filenames:
        name = names_by_filename.get(filename)
        if name:
            names.append(name)
        else:
            missing.append(filename)
    return names, missing


def find_activities_by_name(activities: list[dict], names: list[str]) -> tuple[list[dict], list[str]]:
    """Find activities by exact name, case-insensitively. Returns (matches, missing names)."""
    activities_by_name: dict[str, list[dict]] = {}
    for activity in activities:
        name = activity.get("name", "")
        if name:
            activities_by_name.setdefault(name.casefold(), []).append(activity)

    matches = []
    missing = []
    for name in names:
        found = activities_by_name.get(name.casefold(), [])
        if found:
            matches.extend(found)
        else:
            missing.append(name)
    return matches, missing


def unique_delete_targets(targets: list[dict]) -> list[dict]:
    """Deduplicate delete targets by activity ID while preserving order."""
    unique = []
    seen = set()
    for target in targets:
        activity_id = target.get("id")
        if not activity_id or activity_id in seen:
            continue
        unique.append(target)
        seen.add(activity_id)
    return unique


def print_delete_plan(targets: list[dict]):
    for target in targets:
        name = target.get("name")
        if name:
            print(f"  - {name} ({target.get('id')})")
        else:
            print(f"  - {target.get('id')}")


def delete_targets(base_url: str, token: str, timeout: int, targets: list[dict]) -> tuple[int, int]:
    succeeded = 0
    failed = 0

    for i, target in enumerate(targets, 1):
        activity_id = target.get("id")
        label = target.get("name") or activity_id
        print(f"[{i}/{len(targets)}] Deleting {label}...")
        try:
            delete_activity(base_url, token, activity_id, timeout)
            print("  Deleted")
            succeeded += 1
        except Exception as e:
            print(f"  FAILED: {e}")
            failed += 1

    return succeeded, failed


def cmd_delete(args):
    if not args.password:
        print("Error: Admin password is required. Use --password or set SEED_ADMIN_PASSWORD.")
        sys.exit(1)

    if not args.id and not args.name and not args.only:
        print("Error: Provide at least one of --id, --name, or --only.")
        sys.exit(1)

    print(f"\nLogging in as {args.email} at {args.base_url}...")
    token = login(args.base_url, args.email, args.password, args.timeout)
    print("Authenticated successfully.\n")

    targets = [{"id": activity_id} for activity_id in (args.id or [])]

    names_to_delete = list(args.name or [])
    if args.only:
        csv_names, missing_filenames = csv_filenames_to_names(args.only)
        names_to_delete.extend(csv_names)
        if missing_filenames:
            print(f"Warning: {len(missing_filenames)} filename(s) not found in {CSV_PATH.name}:")
            for filename in missing_filenames:
                print(f"  - {filename}")

    if names_to_delete:
        print(f"Fetching activities from {args.base_url}...")
        activities = fetch_activities(args.base_url, args.timeout, token)
        name_matches, missing_names = find_activities_by_name(activities, names_to_delete)
        targets.extend(name_matches)

        if missing_names:
            print(f"Warning: {len(missing_names)} activity name(s) not found on server:")
            for name in missing_names:
                print(f"  - {name}")

    targets = unique_delete_targets(targets)
    if not targets:
        print("No matching activities to delete.")
        sys.exit(1)

    print(f"\nActivities selected for deletion ({len(targets)}):")
    print_delete_plan(targets)

    confirm_or_exit(
        f"\nThis will permanently delete {len(targets)} activity record(s).",
        args.yes,
    )

    succeeded, failed = delete_targets(args.base_url, token, args.timeout, targets)

    print("=" * 60)
    print(f"Delete complete: {succeeded} deleted, {failed} failed")
    print("=" * 60)

    if failed > 0:
        sys.exit(1)


def cmd_delete_all(args):
    if not args.password:
        print("Error: Admin password is required. Use --password or set SEED_ADMIN_PASSWORD.")
        sys.exit(1)

    print(f"\nLogging in as {args.email} at {args.base_url}...")
    token = login(args.base_url, args.email, args.password, args.timeout)
    print("Authenticated successfully.\n")

    print(f"Fetching activities from {args.base_url}...")
    activities = fetch_activities(args.base_url, args.timeout, token)
    targets = unique_delete_targets(activities)

    if not targets:
        print("No activities to delete.")
        return

    print(f"\nAll activities selected for deletion ({len(targets)}):")
    print_delete_plan(targets)

    confirm_or_exit(
        f"\nThis will permanently delete all {len(targets)} activity record(s).",
        args.yes,
    )

    succeeded, failed = delete_targets(args.base_url, token, args.timeout, targets)

    print("=" * 60)
    print(f"Delete-all complete: {succeeded} deleted, {failed} failed")
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

    # -- delete --
    delete_parser = subparsers.add_parser("delete", help="Delete selected activities")
    delete_parser.add_argument(
        "--email",
        default=os.environ.get("SEED_ADMIN_EMAIL", "admin@learnhub.com"),
        help="Admin email (default: $SEED_ADMIN_EMAIL or admin@learnhub.com)",
    )
    delete_parser.add_argument(
        "--password",
        default=os.environ.get("SEED_ADMIN_PASSWORD"),
        help="Admin password (default: $SEED_ADMIN_PASSWORD)",
    )
    delete_parser.add_argument(
        "--id",
        nargs="+",
        metavar="UUID",
        help="Delete activities by server UUID",
    )
    delete_parser.add_argument(
        "--name",
        nargs="+",
        metavar="NAME",
        help="Delete activities by exact activity name (quote names containing spaces)",
    )
    delete_parser.add_argument(
        "--only",
        nargs="+",
        metavar="FILENAME",
        help="Delete activities whose names match these CSV PDF filenames",
    )
    delete_parser.add_argument(
        "-y", "--yes",
        action="store_true",
        help="Skip the confirmation prompt",
    )

    # -- delete-all --
    delete_all_parser = subparsers.add_parser("delete-all", help="Delete all activities")
    delete_all_parser.add_argument(
        "--email",
        default=os.environ.get("SEED_ADMIN_EMAIL", "admin@learnhub.com"),
        help="Admin email (default: $SEED_ADMIN_EMAIL or admin@learnhub.com)",
    )
    delete_all_parser.add_argument(
        "--password",
        default=os.environ.get("SEED_ADMIN_PASSWORD"),
        help="Admin password (default: $SEED_ADMIN_PASSWORD)",
    )
    delete_all_parser.add_argument(
        "-y", "--yes",
        action="store_true",
        help="Skip the confirmation prompt",
    )

    args = parser.parse_args()
    require_requests()

    if args.command == "seed":
        cmd_seed(args)
    elif args.command == "export":
        cmd_export(args)
    elif args.command == "delete":
        cmd_delete(args)
    elif args.command == "delete-all":
        cmd_delete_all(args)


if __name__ == "__main__":
    main()
