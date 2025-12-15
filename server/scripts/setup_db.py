#!/usr/bin/env python3
"""
Unified database setup script for LEARN-Hub.
Consolidates initialization, mock data population, and dataset import.
"""

from __future__ import annotations

import argparse
import csv
import logging
import sys
from pathlib import Path

# Add the server directory to the Python path
server_dir = Path(__file__).parent.parent
sys.path.insert(0, str(server_dir))

# Add scripts directory to path for local imports
scripts_dir = Path(__file__).parent
sys.path.insert(0, str(scripts_dir))

from utils import (
    admin_email,
    create_placeholder_pdf,
    ensure_admin_user,
    get_demo_activities,
    parse_list,
    validate_resources,
)

from app.core.models import ActivityFormat, BloomLevel, EnergyLevel
from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.db.models.user import PDFDocument
from app.services.pdf_service import PDFService

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


# ============================================================================
# MOCK DATA MODE
# ============================================================================


def populate_mock_data():
    """Populate the database with mock demo data."""
    logger.info("Populating database with mock data...")

    # Get database session
    session = get_db_session()

    try:
        # Check if data already exists
        existing_activities = session.query(Activity).count()
        existing_documents = session.query(PDFDocument).count()

        # Create or use existing PDF document
        if existing_documents == 0:
            # Generate placeholder PDF and store it
            pdf_content = create_placeholder_pdf()
            pdf_service = PDFService()
            document_id = pdf_service.store_pdf(pdf_content, "demo_activities_placeholder.pdf")
            logger.info(f"Created demo placeholder PDF with document ID: {document_id}")
        else:
            # Use existing document
            document_id = session.query(PDFDocument).first().id
            logger.info(f"Using existing PDF document (ID: {document_id})")

        if existing_activities > 0:
            logger.info(f"Database already contains {existing_activities} activities. Skipping activity creation.")
        else:
            # Create activities with document_id and description
            activities = get_demo_activities()
            for activity in activities:
                activity.document_id = document_id
                # Add default description if not set
                if not hasattr(activity, "description") or not activity.description:
                    activity.description = (
                        f"Demo activity: {activity.name} - A comprehensive educational activity "
                        f"designed for students aged {activity.age_min}-{activity.age_max} years."
                    )
                session.add(activity)
            logger.info(f"Created {len(activities)} demo activities")

        # Commit changes
        session.commit()

        # Create admin user
        admin_password = ensure_admin_user(session)

        logger.info("Mock data populated successfully!")
        logger.info("\n" + "=" * 60)
        logger.info("ADMIN CREDENTIALS")
        logger.info("=" * 60)
        logger.info(f"Email: {admin_email}")
        logger.info(f"Password: {admin_password}")
        logger.info("=" * 60)

    except Exception as e:
        session.rollback()
        logger.error(f"Error populating mock data: {e}")
        raise
    finally:
        session.close()


# ============================================================================
# DATASET IMPORT MODE
# ============================================================================


def import_dataset(csv_path: Path, pdf_dir: Path):
    """Import data from CSV and PDFs."""
    logger.info(f"Importing dataset from CSV: {csv_path}")
    logger.info(f"Reading PDFs from: {pdf_dir}")

    if not csv_path.exists():
        logger.error(f"CSV file not found: {csv_path}")
        sys.exit(1)

    if not pdf_dir.exists():
        logger.error(f"PDF directory not found: {pdf_dir}")
        sys.exit(1)

    session = get_db_session()
    pdf_service = PDFService()

    try:
        with open(csv_path, encoding="utf-8") as f:
            reader = csv.DictReader(f)

            count = 0
            for row in reader:
                filename = row["filename"].strip()
                pdf_path = pdf_dir / filename

                if not pdf_path.exists():
                    logger.warning(f"PDF file not found for activity '{row['name']}': {pdf_path}. Skipping.")
                    continue

                logger.info(f"Processing activity: {row['name']}")

                # Read PDF content
                with open(pdf_path, "rb") as pdf_file:
                    pdf_content = pdf_file.read()

                # Store PDF using PDFService
                try:
                    document_id = pdf_service.store_pdf(pdf_content, filename)
                    logger.info(f"PDF stored with document_id: {document_id}")
                except Exception as e:
                    logger.error(f"Failed to store PDF for '{filename}': {e}")
                    raise

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

            # Create admin user
            admin_password = ensure_admin_user(session)
            logger.info("\n" + "=" * 60)
            logger.info("ADMIN CREDENTIALS")
            logger.info("=" * 60)
            logger.info(f"Email: {admin_email}")
            logger.info(f"Password: {admin_password}")
            logger.info("=" * 60 + "\n")

    except Exception as e:
        session.rollback()
        logger.error(f"Error importing dataset: {e}")
        raise
    finally:
        session.close()


# ============================================================================
# MAIN CLI
# ============================================================================


def main():
    """Main entry point for the database population script."""
    parser = argparse.ArgumentParser(
        description="Database population script for LEARN-Hub. Run 'make db-setup' first.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Prerequisites:
  Run 'make db-setup' first to apply Alembic migrations.

Examples:
  # Populate with mock data
  python scripts/setup_db.py mock

  # Import real dataset
  python scripts/setup_db.py dataset --csv ../dataset/dataset.csv --pdf-dir ../dataset/pdfs
        """,
    )

    subparsers = parser.add_subparsers(dest="mode", required=True, help="Operation mode")

    # Mock mode - seed mock data
    subparsers.add_parser("mock", help="Populate database with mock data")

    # Dataset mode - import real data
    dataset_parser = subparsers.add_parser("dataset", help="Import real dataset from CSV and PDFs")
    dataset_parser.add_argument(
        "--csv",
        type=Path,
        default=Path("../dataset/dataset.csv"),
        help="Path to the CSV file containing activity data (default: ../dataset/dataset.csv)",
    )
    dataset_parser.add_argument(
        "--pdf-dir",
        type=Path,
        default=Path("../dataset/pdfs"),
        help="Path to the directory containing PDF files (default: ../dataset/pdfs)",
    )

    args = parser.parse_args()

    # Execute mode-specific operations
    if args.mode == "mock":
        logger.info("Populating database with mock data...")
        populate_mock_data()

    elif args.mode == "dataset":
        # Resolve paths relative to script location
        csv_path = args.csv if args.csv.is_absolute() else (server_dir / args.csv).resolve()
        pdf_dir = args.pdf_dir if args.pdf_dir.is_absolute() else (server_dir / args.pdf_dir).resolve()
        import_dataset(csv_path, pdf_dir)

    logger.info("✓ Setup complete!")


if __name__ == "__main__":
    main()
