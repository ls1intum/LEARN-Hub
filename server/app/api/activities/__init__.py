"""Activities API module."""

from __future__ import annotations

# Re-export PDF service for backward-compatible test patch targets
from app.services.pdf_service import PDFService as PDFService

from . import (
    creation,
    lesson_plan,
    listing,
    pdf,
    recommendations,
)

__all__ = [
    "PDFService",
    "creation",
    "lesson_plan",
    "listing",
    "pdf",
    "recommendations",
]
