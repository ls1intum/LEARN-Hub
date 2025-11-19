#!/usr/bin/env python3
"""
Import demo data from CSV and PDFs.
This script reads a CSV file containing activity data and a directory of PDF files,
and populates the database.
"""

import argparse
import csv
import logging
import sys
from pathlib import Path
from typing import Any

# Add the server directory to the Python path
server_dir = Path(__file__).parent.parent
sys.path.insert(0, str(server_dir))

from app.core.models import ActivityFormat, ActivityResource, BloomLevel, EnergyLevel
from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.services.pdf_service import PDFService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def parse_arguments():
    parser = argparse.ArgumentParser(description="Import demo data from CSV and PDFs")
    parser.add_argument(
        "--csv",
        type=Path,
        default=Path("quick_start.csv"),
        help="Path to the CSV file containing activity data",
    )
    parser.add_argument(
        "--pdf-dir",
        type=Path,
        default=Path("."),
        help="Path to the directory containing PDF files",
    )
    return parser.parse_args()


def parse_list(value: str) -> list[str]:
    """Parse pipe-separated list string."""
    if not value:
        return []
    return [item.strip() for item in value.split("|") if item.strip()]


def validate_resources(resources: list[str]) -> list[str]:
    """Filter resources to only include valid ActivityResource values."""
    valid_resources = {r.value for r in ActivityResource}
    validated = []
    for r in resources:
        if r.lower() in valid_resources:
            validated.append(r.lower())
        else:
            logger.warning(f"Ignoring invalid resource: {r}. Valid resources are: {valid_resources}")
    return validated


def import_data(csv_path: Path, pdf_dir: Path):
    """Import data from CSV and PDFs."""
    if not csv_path.exists():
        logger.error(f"CSV file not found: {csv_path}")
        sys.exit(1)

    if not pdf_dir.exists():
        logger.error(f"PDF directory not found: {pdf_dir}")
        sys.exit(1)

    session = get_db_session()
    pdf_service = PDFService()
    
    try:
        with open(csv_path, "r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            
            count = 0
            for row in reader:
                filename = row["filename"].strip()
                pdf_path = pdf_dir / filename
                
                if not pdf_path.exists():
                    logger.warning(f"PDF file not found for activity '{row['name']}': {pdf_path}. Skipping.")
                    continue
                
                logger.info(f"Processing activity: {row['name']}")
                
                # Store PDF
                with open(pdf_path, "rb") as pdf_file:
                    pdf_content = pdf_file.read()
                    document_id = pdf_service.store_pdf(pdf_content, filename)
                
                # Parse and validate resources
                raw_resources = parse_list(row["resources_needed"])
                validated_resources = validate_resources(raw_resources)

                # Create Activity
                activity = Activity(
                    name=row["name"],
                    source=row["source"],
                    age_min=int(row["age_min"]),
                    age_max=int(row["age_max"]),
                    format=ActivityFormat(row["format"].lower()).value,
                    resources_needed=validated_resources,
                    bloom_level=BloomLevel(row["bloom_level"].lower()).value,
                    duration_min_minutes=int(row["duration_min_minutes"]),
                    duration_max_minutes=int(row["duration_max_minutes"]),
                    mental_load=EnergyLevel(row["mental_load"].lower()),
                    physical_energy=EnergyLevel(row["physical_energy"].lower()),
                    prep_time_minutes=int(row["prep_time_minutes"]),
                    cleanup_time_minutes=int(row["cleanup_time_minutes"]),
                    topics=parse_list(row["topics"]),
                    description=row["description"],
                    document_id=document_id,
                )
                
                session.add(activity)
                count += 1
            
            session.commit()
            logger.info(f"Successfully imported {count} activities.")
            
    except Exception as e:
        session.rollback()
        logger.error(f"Error importing data: {e}")
        raise
    finally:
        session.close()


if __name__ == "__main__":
    args = parse_arguments()
    import_data(args.csv, args.pdf_dir)
