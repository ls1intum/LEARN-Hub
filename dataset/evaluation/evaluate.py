#!/usr/bin/env python3
"""
Create LEARN-Hub draft activities for the evaluation subset and download each
generated combined activity PDF.

Usage:
    python create_drafts_and_download_pdfs.py --password <admin-password>

Environment variables:
    EVAL_BASE_URL        Server base URL        (default: http://localhost:5001)
    EVAL_ADMIN_EMAIL     Admin email            (default: admin@learnhub.com)
    EVAL_ADMIN_PASSWORD  Admin password         (required unless --password is set)
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from pathlib import Path

try:
    import requests
except ImportError:
    requests = None


SCRIPT_DIR = Path(__file__).resolve().parent
DATASET_DIR = SCRIPT_DIR.parent
PDF_DIR = DATASET_DIR / "pdfs"
DEFAULT_OUTPUT_DIR = SCRIPT_DIR / "output" / "activity-pdfs"

EVALUATION_PDFS = [
    # "LEARN-Kit-algorithmen_und_roboter.pdf",
    # "LEARN-Kit-netzwerk.pdf",
    # "barefoot-howcomputerslearn.pdf",
    # "csunplugged-colourbynumbers.pdf",
    # "codeorg-artist.pdf",
    # "learnlabs-logik.pdf",
    "LEARN-Kit-binaersystem.pdf",
    "learnlabs-baeume.pdf",
    "barefoot-worldmap.pdf",
    "learnlabs-itsicherheit.pdf",
    "barefoot-patternsunplugges.pdf",
    "csunplugged-squeezingpictures.pdf",
]

MARKDOWN_TYPES = [
    "cover_sheet",
    "lesson_plan",
    "background_knowledge",
    "board_image",
    "exercise",
    "exercise_solution",
]


def require_requests():
    if requests is None:
        print("Error: 'requests' package is required. Install it with: pip install requests")
        sys.exit(1)


def csrf_headers(session: requests.Session) -> dict[str, str]:
    token = session.cookies.get("XSRF-TOKEN")
    if not token:
        return {}
    return {"X-XSRF-TOKEN": token}


def fetch_csrf_token(session: requests.Session, base_url: str, timeout: int):
    resp = session.get(f"{base_url}/api/auth/csrf", timeout=timeout)
    if resp.status_code != 200:
        raise RuntimeError(f"CSRF token fetch failed ({resp.status_code}): {resp.text}")


def login(session: requests.Session, base_url: str, email: str, password: str, timeout: int):
    fetch_csrf_token(session, base_url, timeout)
    resp = session.post(
        f"{base_url}/api/auth/login",
        json={"email": email, "password": password},
        headers=csrf_headers(session),
        timeout=timeout,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"Login failed ({resp.status_code}): {resp.text}")

    user = resp.json().get("user", resp.json())
    if user.get("role") != "ADMIN":
        raise RuntimeError(f"Authenticated user is not an admin: {user.get('email', email)}")


def upload_and_create_draft(session: requests.Session, base_url: str, pdf_path: Path, timeout: int) -> dict:
    data = [("generateMetadata", "true")]
    data.extend(("markdownTypes", markdown_type) for markdown_type in MARKDOWN_TYPES)

    with open(pdf_path, "rb") as pdf_file:
        resp = session.post(
            f"{base_url}/api/activities/upload-and-create-pending",
            files={"pdf_file": (pdf_path.name, pdf_file, "application/pdf")},
            data=data,
            headers=csrf_headers(session),
            timeout=timeout,
        )

    if resp.status_code != 201:
        raise RuntimeError(f"Draft upload failed ({resp.status_code}): {resp.text}")
    return resp.json()


def get_activity(session: requests.Session, base_url: str, activity_id: str, timeout: int) -> dict:
    resp = session.get(
        f"{base_url}/api/activities/{activity_id}",
        timeout=timeout,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"Fetch activity failed ({resp.status_code}): {resp.text}")
    return resp.json()


def wait_for_draft(
    base_url: str,
    session: requests.Session,
    activity_id: str,
    timeout: int,
    wait_timeout: int,
    poll_interval: int,
) -> dict:
    deadline = time.time() + wait_timeout

    while True:
        activity = get_activity(session, base_url, activity_id, timeout)
        status = activity.get("status")
        generation_error = activity.get("generationError")

        if status == "DRAFT":
            return activity
        if generation_error:
            raise RuntimeError(f"Background generation failed: {generation_error}")
        if time.time() >= deadline:
            raise RuntimeError(f"Timed out after {wait_timeout}s waiting for draft generation")

        time.sleep(poll_interval)


def download_activity_pdf(
    session: requests.Session,
    base_url: str,
    activity_id: str,
    output_path: Path,
    timeout: int,
):
    resp = session.get(f"{base_url}/api/activities/{activity_id}/pdf", timeout=timeout)
    if resp.status_code != 200:
        raise RuntimeError(f"PDF download failed ({resp.status_code}): {resp.text}")

    content_type = resp.headers.get("content-type", "")
    if "application/pdf" not in content_type.lower():
        raise RuntimeError(f"PDF download returned unexpected content type: {content_type}")

    output_path.write_bytes(resp.content)


def safe_filename(value: str) -> str:
    stem = Path(value).stem
    stem = re.sub(r"[^A-Za-z0-9._-]+", "-", stem).strip("-._")
    return stem or "activity"


def create_evaluation_draft(
    base_url: str,
    session: requests.Session,
    pdf_filename: str,
    output_dir: Path,
    request_timeout: int,
    download_timeout: int,
    wait_timeout: int,
    poll_interval: int,
) -> dict:
    pdf_path = PDF_DIR / pdf_filename
    if not pdf_path.exists():
        raise FileNotFoundError(f"Missing evaluation PDF: {pdf_path}")

    print(f"Uploading {pdf_filename}...")
    created = upload_and_create_draft(session, base_url, pdf_path, request_timeout)
    activity_id = created["id"]
    print(f"  Created {activity_id}; waiting for DRAFT status...")

    activity = wait_for_draft(
        base_url,
        session,
        activity_id,
        request_timeout,
        wait_timeout,
        poll_interval,
    )

    activity_name = activity.get("name") or Path(pdf_filename).stem
    output_path = output_dir / f"{safe_filename(activity_name)}-{activity_id}.pdf"
    print(f"  Downloading combined activity PDF...")
    download_activity_pdf(session, base_url, activity_id, output_path, download_timeout)
    print(f"  Saved {output_path}")

    return {
        "sourcePdf": pdf_filename,
        "activityId": activity_id,
        "activityName": activity_name,
        "status": activity.get("status"),
        "generationError": activity.get("generationError"),
        "downloadedPdf": str(output_path),
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Create evaluation drafts and download each combined activity PDF."
    )
    parser.add_argument(
        "--base-url",
        default=os.environ.get("EVAL_BASE_URL", "http://localhost:5001"),
        help="LEARN-Hub server base URL",
    )
    parser.add_argument(
        "--email",
        default=os.environ.get("EVAL_ADMIN_EMAIL", "admin@learnhub.com"),
        help="Admin email",
    )
    parser.add_argument(
        "--password",
        default=os.environ.get("EVAL_ADMIN_PASSWORD"),
        help="Admin password",
    )
    parser.add_argument(
        "--output-dir",
        default=str(DEFAULT_OUTPUT_DIR),
        help="Directory where combined activity PDFs are written",
    )
    parser.add_argument(
        "--only",
        action="append",
        choices=EVALUATION_PDFS,
        help="Limit the run to one evaluation PDF. Can be provided multiple times.",
    )
    parser.add_argument(
        "--request-timeout",
        type=int,
        default=60,
        help="Timeout in seconds for normal API requests",
    )
    parser.add_argument(
        "--download-timeout",
        type=int,
        default=180,
        help="Timeout in seconds for combined PDF downloads",
    )
    parser.add_argument(
        "--wait-timeout",
        type=int,
        default=1800,
        help="Maximum seconds to wait for each draft generation",
    )
    parser.add_argument(
        "--poll-interval",
        type=int,
        default=5,
        help="Seconds between draft status checks",
    )
    return parser.parse_args()


def main():
    args = parse_args()
    require_requests()

    if not args.password:
        print("Error: Admin password is required. Use --password or set EVAL_ADMIN_PASSWORD.")
        sys.exit(1)

    selected_pdfs = args.only or EVALUATION_PDFS
    output_dir = Path(args.output_dir).resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    print(f"Logging in as {args.email} at {args.base_url}...")
    session = requests.Session()
    login(session, args.base_url, args.email, args.password, args.request_timeout)
    print(f"Authenticated. Running evaluation subset with {len(selected_pdfs)} PDF(s).\n")

    results = []
    failed = 0

    for index, pdf_filename in enumerate(selected_pdfs, 1):
        print(f"[{index}/{len(selected_pdfs)}] {pdf_filename}")
        try:
            result = create_evaluation_draft(
                args.base_url,
                session,
                pdf_filename,
                output_dir,
                args.request_timeout,
                args.download_timeout,
                args.wait_timeout,
                args.poll_interval,
            )
            results.append(result)
        except Exception as exc:
            failed += 1
            result = {
                "sourcePdf": pdf_filename,
                "status": "FAILED",
                "error": str(exc),
            }
            results.append(result)
            print(f"  FAILED: {exc}")
        print()

    manifest_path = output_dir.parent / "evaluation-run-manifest.json"
    manifest_path.write_text(json.dumps(results, indent=2, ensure_ascii=False), encoding="utf-8")

    succeeded = len(selected_pdfs) - failed
    print("=" * 60)
    print(f"Evaluation draft run complete: {succeeded} succeeded, {failed} failed")
    print(f"PDF output directory: {output_dir}")
    print(f"Run manifest: {manifest_path}")
    print("=" * 60)

    if failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
