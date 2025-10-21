from __future__ import annotations

import io
import logging
from datetime import datetime
from pathlib import Path
from typing import Any

from pypdf import PdfReader, PdfWriter
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from app.db.database import get_db_session
from app.db.models.user import PDFDocument
from app.utils.config import Config

logger = logging.getLogger(__name__)


class PDFService:
    """Minimal PDF service for storing and retrieving PDF documents."""

    def __init__(self):
        config = Config.get_instance()
        self.storage_path = Path(config.pdf_storage_path)
        self.max_file_size = 100 * 1024 * 1024  # 100MB
        self._ensure_storage_directory()

    def _ensure_storage_directory(self) -> None:
        """Ensure the PDF storage directory exists and is writable."""
        try:
            self.storage_path.mkdir(parents=True, exist_ok=True)
            # Test write permissions
            test_file = self.storage_path / ".test_write"
            test_file.touch()
            test_file.unlink()
            logger.info(f"PDF storage directory ready: {self.storage_path}")
        except Exception as e:
            logger.error(f"Failed to setup PDF storage directory: {e}")
            raise RuntimeError(f"Cannot access PDF storage directory: {self.storage_path}") from e

    def store_pdf(self, pdf_content: bytes, filename: str) -> int:
        """
        Store a PDF file in database and filesystem, return database document ID.

        Args:
            pdf_content: The PDF file content as bytes
            filename: Original filename of the PDF

        Returns:
            Database document ID (integer)
        """
        try:
            if len(pdf_content) > self.max_file_size:
                raise ValueError(f"PDF too large: {len(pdf_content)} bytes (max: {self.max_file_size})")

            if not pdf_content.startswith(b"%PDF-"):
                raise ValueError("Invalid PDF content")
            db = get_db_session()
            try:
                pdf_document = PDFDocument(
                    filename=filename,
                    file_path="",
                    file_size=len(pdf_content),
                    extracted_fields=None,
                    confidence_score=None,
                    extraction_quality=None,
                )

                db.add(pdf_document)
                db.flush()
                document_id = pdf_document.id

                file_path = self._get_document_path(document_id, filename)
                with open(file_path, "wb") as f:
                    f.write(pdf_content)

                pdf_document.file_path = str(file_path)
                db.commit()

                logger.info(f"PDF document stored successfully with database ID {document_id}")
                return document_id

            except Exception:
                db.rollback()
                try:
                    file_path = self._get_document_path(document_id, filename)
                    if file_path.exists():
                        file_path.unlink()
                except Exception:
                    pass
                raise

        except Exception as e:
            logger.error(f"Failed to store PDF document: {e}")
            raise

    def get_pdf_content(self, document_id: int) -> bytes | None:
        """Retrieve the PDF file content by database document ID."""
        try:
            db = get_db_session()
            pdf_document = db.query(PDFDocument).filter(PDFDocument.id == document_id).first()

            if not pdf_document:
                logger.warning(f"PDF document {document_id} not found in database")
                return None

            file_path = Path(pdf_document.file_path)

            if not file_path.exists():
                logger.warning(f"PDF file not found at path {file_path} for document {document_id}")
                return None

            with open(file_path, "rb") as f:
                content = f.read()

            logger.debug(f"PDF content retrieved for document {document_id}")
            return content

        except Exception as e:
            logger.error(f"Failed to retrieve PDF content for document {document_id}: {e}")
            return None

    def get_pdf_info(self, document_id: int) -> dict[str, Any] | None:
        """Get basic information about a PDF document from database."""
        try:
            db = get_db_session()
            pdf_document = db.query(PDFDocument).filter(PDFDocument.id == document_id).first()

            if not pdf_document:
                logger.warning(f"PDF document {document_id} not found in database")
                return None

            return {
                "id": pdf_document.id,
                "filename": pdf_document.filename,
                "file_size": pdf_document.file_size,
                "file_path": pdf_document.file_path,
                "extracted_fields": pdf_document.extracted_fields,
                "confidence_score": pdf_document.confidence_score,
                "extraction_quality": pdf_document.extraction_quality,
                "created_at": pdf_document.created_at.isoformat() if pdf_document.created_at else None,
            }

        except Exception as e:
            logger.error(f"Failed to get PDF info for document {document_id}: {e}")
            return None

    def update_pdf_extraction_results(
        self,
        document_id: int,
        extracted_fields: dict[str, Any] | None = None,
        confidence_score: str | None = None,
        extraction_quality: str | None = None,
    ) -> bool:
        """Update PDF document with extraction results."""
        db = None
        try:
            db = get_db_session()
            pdf_document = db.query(PDFDocument).filter(PDFDocument.id == document_id).first()

            if not pdf_document:
                logger.warning(f"PDF document {document_id} not found in database")
                return False

            if extracted_fields is not None:
                pdf_document.extracted_fields = extracted_fields
            if confidence_score is not None:
                pdf_document.confidence_score = confidence_score
            if extraction_quality is not None:
                pdf_document.extraction_quality = extraction_quality

            db.commit()
            logger.info(f"Updated PDF document {document_id} with extraction results")
            return True

        except Exception as e:
            logger.error(f"Failed to update PDF document {document_id}: {e}")
            if db:
                db.rollback()
            return False

    def _generate_document_id(self) -> int:
        """Generate a unique document ID - DEPRECATED: Use database IDs instead."""
        import random
        import time

        return int(time.time() * 1000) + random.randint(1000, 9999)

    def _get_document_path(self, document_id: int, filename: str) -> Path:
        """Get the file path for a PDF document using database ID."""
        safe_filename = "".join(c for c in filename if c.isalnum() or c in (" ", "-", "_", ".")).rstrip()
        return self.storage_path / f"doc_{document_id}_{safe_filename}"

    def _find_document_file(self, document_id: int) -> Path | None:
        """Find the file for a given document ID - DEPRECATED: Use database lookup instead."""
        pattern = f"doc_{document_id}_*"
        matches = list(self.storage_path.glob(pattern))
        return matches[0] if matches else None

    def _get_test_document(self) -> bytes | None:
        """Get the test document content for testing purposes - DEPRECATED."""
        test_file_path = self.storage_path / "doc_1_test_upload.pdf"
        if test_file_path.exists():
            with open(test_file_path, "rb") as f:
                return f.read()
        return None

    # ===== LESSON PLAN GENERATION =====

    def generate_lesson_plan(
        self,
        activities: list[dict[str, Any]],
        search_criteria: dict[str, Any],
        breaks: list[dict[str, Any]],
        total_duration: int,
    ) -> bytes:
        """
        Generate a complete lesson plan PDF with summary page and activity PDFs.

        Args:
            activities: List of activity dictionaries
            search_criteria: Original search criteria used
            breaks: List of break information
            total_duration: Total duration in minutes

        Returns:
            Complete lesson plan as PDF bytes
        """
        try:
            output_buffer = io.BytesIO()

            summary_pdf = self._generate_summary_page(activities, search_criteria, breaks, total_duration)
            activity_pdfs = self._get_activity_pdfs([activity["id"] for activity in activities])
            self._merge_pdfs(output_buffer, [summary_pdf] + activity_pdfs)

            output_buffer.seek(0)
            return output_buffer.getvalue()

        except Exception as e:
            logger.error(f"Failed to generate lesson plan: {e}")
            raise RuntimeError(f"Failed to generate lesson plan: {e}") from e

    def get_lesson_plan_info(self, activities: list[dict[str, Any]]) -> dict[str, Any]:
        """Get information about what would be included in a lesson plan."""
        total_activities = len(activities)
        total_duration = sum(
            activity.get("duration_min_minutes", 0)
            + activity.get("prep_time_minutes", 5)
            + activity.get("cleanup_time_minutes", 5)
            for activity in activities
        )

        pdf_count = sum(1 for activity in activities if self.pdf_exists_for_activity(activity["id"]))

        # Extract unique values for client compatibility
        topics_covered = list(set(topic for activity in activities for topic in activity.get("topics", [])))

        bloom_levels = list(
            set(activity.get("bloom_level", "") for activity in activities if activity.get("bloom_level"))
        )

        formats = list(set(activity.get("format", "") for activity in activities if activity.get("format")))

        # Calculate age range
        ages = [activity.get("age_min", 0) for activity in activities if activity.get("age_min")]
        age_range = f"{min(ages)}-{max(ages)}" if ages else "N/A"

        return {
            # Server-specific fields
            "total_activities": total_activities,
            "activities_with_pdfs": pdf_count,
            "activities_missing_pdfs": total_activities - pdf_count,
            "estimated_duration_minutes": total_duration,
            "can_generate_lesson_plan": pdf_count > 0,
            # Client-compatible fields
            "title": "Lesson Plan",
            "total_duration": total_duration,
            "activity_count": total_activities,
            "topics_covered": topics_covered,
            "bloom_levels": bloom_levels,
            "age_range": age_range,
            "formats": formats,
        }

    def pdf_exists_for_activity(self, activity_id: int) -> bool:
        """Check if a PDF exists for an activity using same logic as get PDF endpoint."""
        from app.db.models.activity import Activity

        try:
            db = get_db_session()
            activity = db.query(Activity).filter(Activity.id == activity_id).first()

            if not activity:
                return False

            # Check if activity has a document_id and PDF exists
            if activity.document_id:
                pdf_content = self.get_pdf_content(activity.document_id)
                if pdf_content:
                    return True

            # TEMPORARY FALLBACK: Check if fallback PDF (ID 999) exists
            pdf_content = self.get_pdf_content(999)
            return pdf_content is not None

        except Exception as e:
            logger.error(f"Failed to check PDF existence for activity {activity_id}: {e}")
            return False

    def _get_activity_pdfs(self, activity_ids: list[int]) -> list[bytes]:
        """Retrieve PDFs for the given activity IDs."""
        pdfs = []
        for activity_id in activity_ids:
            pdf_content = self._get_activity_pdf_content(activity_id)
            if pdf_content:
                pdfs.append(pdf_content)
            else:
                logger.warning(f"PDF not found for activity {activity_id}, skipping")
        return pdfs

    def _get_activity_pdf_content(self, activity_id: int) -> bytes | None:
        """Get PDF content for a specific activity using same logic as get PDF endpoint."""
        from app.db.models.activity import Activity

        try:
            db = get_db_session()
            activity = db.query(Activity).filter(Activity.id == activity_id).first()

            if not activity:
                logger.warning(f"Activity {activity_id} not found")
                return None

            pdf_content = None

            # Try to get the activity's PDF first using document_id
            if activity.document_id:
                pdf_content = self.get_pdf_content(activity.document_id)
                if pdf_content:
                    logger.debug(f"Found PDF for activity {activity_id} via document_id {activity.document_id}")
                    return pdf_content

            # TEMPORARY FALLBACK: If no PDF found, use PDF with ID 999 (same as get PDF endpoint)
            if pdf_content is None:
                pdf_content = self.get_pdf_content(999)
                if pdf_content:
                    logger.info(f"Using fallback PDF (ID 999) for activity {activity_id}")
                    return pdf_content
                else:
                    logger.warning(f"No fallback PDF available for activity {activity_id}")
                    return None

        except Exception as e:
            logger.error(f"Failed to get PDF content for activity {activity_id}: {e}")
            return None

    def _merge_pdfs(self, output_buffer: io.BytesIO, pdf_contents: list[bytes]) -> None:
        """Merge multiple PDF contents into a single PDF."""
        writer = PdfWriter()

        for pdf_content in pdf_contents:
            if pdf_content:
                try:
                    reader = PdfReader(io.BytesIO(pdf_content))
                    for page in reader.pages:
                        writer.add_page(page)
                except Exception as e:
                    logger.error(f"Failed to read PDF content: {e}")
                    continue

        writer.write(output_buffer)

    def _generate_summary_page(
        self,
        activities: list[dict[str, Any]],
        search_criteria: dict[str, Any],
        breaks: list[dict[str, Any]],
        total_duration: int,
    ) -> bytes:
        """Generate a summary page for the lesson plan."""
        buffer = io.BytesIO()
        doc = SimpleDocTemplate(buffer, pagesize=A4)
        story = []

        # Title
        title_style = ParagraphStyle(
            "Title",
            parent=getSampleStyleSheet()["Title"],
            fontSize=18,
            spaceAfter=30,
            alignment=1,  # Center alignment
        )
        story.append(Paragraph("Lesson Plan Summary", title_style))
        story.append(Spacer(1, 20))

        # Search criteria
        story.append(Paragraph("Search Criteria:", getSampleStyleSheet()["Heading2"]))
        criteria_data = []
        for key, value in search_criteria.items():
            if value is not None and value != "":
                criteria_data.append([key.replace("_", " ").title(), str(value)])

        if criteria_data:
            criteria_table = Table(criteria_data, colWidths=[2 * inch, 3 * inch])
            criteria_table.setStyle(
                TableStyle(
                    [
                        ("BACKGROUND", (0, 0), (-1, 0), colors.grey),
                        ("TEXTCOLOR", (0, 0), (-1, 0), colors.whitesmoke),
                        ("ALIGN", (0, 0), (-1, -1), "LEFT"),
                        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                        ("FONTSIZE", (0, 0), (-1, 0), 12),
                        ("BOTTOMPADDING", (0, 0), (-1, 0), 12),
                        ("BACKGROUND", (0, 1), (-1, -1), colors.beige),
                        ("GRID", (0, 0), (-1, -1), 1, colors.black),
                    ]
                )
            )
            story.append(criteria_table)

        story.append(Spacer(1, 20))

        # Activities summary
        story.append(Paragraph("Activities:", getSampleStyleSheet()["Heading2"]))
        activity_data = [["#", "Name", "Duration", "Format", "Bloom Level"]]
        for i, activity in enumerate(activities, 1):
            activity_data.append(
                [
                    str(i),
                    activity.get("name", "N/A"),
                    f"{activity.get('duration_min_minutes', 0)} min",
                    activity.get("format", "N/A"),
                    activity.get("bloom_level", "N/A"),
                ]
            )

        activity_table = Table(activity_data, colWidths=[0.5 * inch, 2.5 * inch, 1 * inch, 1 * inch, 1 * inch])
        activity_table.setStyle(
            TableStyle(
                [
                    ("BACKGROUND", (0, 0), (-1, 0), colors.grey),
                    ("TEXTCOLOR", (0, 0), (-1, 0), colors.whitesmoke),
                    ("ALIGN", (0, 0), (-1, -1), "LEFT"),
                    ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                    ("FONTSIZE", (0, 0), (-1, 0), 10),
                    ("BOTTOMPADDING", (0, 0), (-1, 0), 12),
                    ("BACKGROUND", (0, 1), (-1, -1), colors.beige),
                    ("GRID", (0, 0), (-1, -1), 1, colors.black),
                ]
            )
        )
        story.append(activity_table)

        # Breaks summary
        if breaks:
            story.append(Spacer(1, 20))
            story.append(Paragraph("Breaks:", getSampleStyleSheet()["Heading2"]))
            break_data = [["Duration", "Description"]]
            for break_item in breaks:
                break_data.append(
                    [
                        f"{break_item.get('duration', 0)} min",
                        break_item.get("description", "N/A"),
                    ]
                )

            break_table = Table(break_data, colWidths=[1 * inch, 4 * inch])
            break_table.setStyle(
                TableStyle(
                    [
                        ("BACKGROUND", (0, 0), (-1, 0), colors.grey),
                        ("TEXTCOLOR", (0, 0), (-1, 0), colors.whitesmoke),
                        ("ALIGN", (0, 0), (-1, -1), "LEFT"),
                        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
                        ("FONTSIZE", (0, 0), (-1, 0), 10),
                        ("BOTTOMPADDING", (0, 0), (-1, 0), 12),
                        ("BACKGROUND", (0, 1), (-1, -1), colors.beige),
                        ("GRID", (0, 0), (-1, -1), 1, colors.black),
                    ]
                )
            )
            story.append(break_table)

        # Total duration
        story.append(Spacer(1, 20))
        story.append(Paragraph(f"Total Duration: {total_duration} minutes", getSampleStyleSheet()["Heading2"]))

        # Generated timestamp
        story.append(Spacer(1, 20))
        story.append(
            Paragraph(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}", getSampleStyleSheet()["Normal"])
        )

        doc.build(story)
        buffer.seek(0)
        return buffer.getvalue()
