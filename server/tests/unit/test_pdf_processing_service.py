"""
Unit tests for PDFService - focused on core business logic only.
"""

import tempfile
from pathlib import Path
from unittest.mock import patch

import pytest

from app.services.pdf_service import PDFService


class TestPDFService:
    """Test PDF service core business functionality."""

    @pytest.fixture
    def temp_storage_path(self):
        """Create a temporary storage directory."""
        with tempfile.TemporaryDirectory() as temp_dir:
            yield Path(temp_dir)

    @pytest.fixture
    def pdf_service(self, temp_storage_path):
        """Create PDFService with mocked dependencies."""
        with patch("app.services.pdf_service.Config.get_instance") as mock_config:
            mock_config.return_value.pdf_storage_path = str(temp_storage_path)
            service = PDFService()
            return service

    def test_store_pdf_validation(self, pdf_service):
        """Test PDF storage validation - critical business logic."""
        # Test invalid content
        with pytest.raises(ValueError, match="Invalid PDF content"):
            pdf_service.store_pdf(b"Not a PDF file", "test.pdf")

        # Test file too large
        large_content = b"x" * (101 * 1024 * 1024)  # 101MB
        with pytest.raises(ValueError, match="PDF too large"):
            pdf_service.store_pdf(large_content, "large.pdf")

    def test_lesson_plan_generation(self, pdf_service, temp_storage_path):
        """Test lesson plan generation - core business feature."""
        activities = [
            {"id": 1, "name": "Activity 1", "duration_min_minutes": 30, "format": "unplugged"},
            {"id": 2, "name": "Activity 2", "duration_min_minutes": 45, "format": "digital"},
        ]
        search_criteria = {"target_age": 12}
        breaks = [{"duration": 10, "description": "Break"}]
        total_duration = 85

        # Create PDF files for activities
        (temp_storage_path / "1_activity1.pdf").write_bytes(b"%PDF-1.4\nActivity 1 PDF")
        (temp_storage_path / "2_activity2.pdf").write_bytes(b"%PDF-1.4\nActivity 2 PDF")

        result = pdf_service.generate_lesson_plan(
            activities=activities, search_criteria=search_criteria, breaks=breaks, total_duration=total_duration
        )

        # Should return PDF bytes
        assert isinstance(result, bytes)
        assert result.startswith(b"%PDF-")

    def test_lesson_plan_info_calculation(self, pdf_service, temp_storage_path):
        """Test lesson plan info calculation - business logic."""
        activities = [
            {"id": 1, "duration_min_minutes": 30, "prep_time_minutes": 5, "cleanup_time_minutes": 5},
            {"id": 2, "duration_min_minutes": 45, "prep_time_minutes": 10, "cleanup_time_minutes": 5},
            {"id": 3, "duration_min_minutes": 20, "prep_time_minutes": 5, "cleanup_time_minutes": 5},
        ]

        # Mock the entire pdf_exists_for_activity method to return True for all activities
        with patch.object(pdf_service, "pdf_exists_for_activity", return_value=True):
            result = pdf_service.get_lesson_plan_info(activities)

        assert result["total_activities"] == 3
        assert result["activities_with_pdfs"] == 3  # All 3 activities have PDFs
        assert result["activities_missing_pdfs"] == 0
        assert result["estimated_duration_minutes"] == 130  # 30+5+5 + 45+10+5 + 20+5+5
        assert result["can_generate_lesson_plan"] is True
