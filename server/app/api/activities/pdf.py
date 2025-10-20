"""Activity PDF API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask import make_response
from flask_openapi3 import Tag

from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.services.pdf_service import PDFService
from app.utils.pydantic_models import ActivityIdPath, ErrorResponse
from app.utils.response_helpers import error_response, not_found_response

# Define API tag for OpenAPI
activities_tag = Tag(name="activities", description="Activity management and recommendations")


def register_activities_pdf_routes(api):
    """Register activity PDF routes with Flask-OpenAPI3."""

    @api.get(
        "/api/activities/<int:activity_id>/pdf",
        tags=[activities_tag],
        responses={200: {}, 404: ErrorResponse, 500: ErrorResponse},
        summary="Get activity PDF",
        description="Get PDF file for a specific activity",
    )
    def get_activity_pdf(path: ActivityIdPath):
        """Get activity PDF."""
        try:
            activity_id = path.activity_id
            db_session = get_db_session()

            # Get the activity
            activity = db_session.query(Activity).filter(Activity.id == activity_id).first()
            if not activity:
                return not_found_response("Activity")

            # Get PDF content using the document_id from the activity
            pdf_service = PDFService()
            pdf_content = None
            filename = f"activity_{activity_id}.pdf"

            # Try to get the activity's PDF first
            if activity.document_id:
                pdf_content = pdf_service.get_pdf_content(activity.document_id)
                if pdf_content:
                    # Get PDF document info for filename
                    pdf_info = pdf_service.get_pdf_info(activity.document_id)
                    filename = pdf_info["filename"] if pdf_info else f"activity_{activity_id}.pdf"

            # TEMPORARY FALLBACK: If no PDF found, use PDF with ID 4
            if pdf_content is None:
                pdf_content = pdf_service.get_pdf_content(4)
                if pdf_content:
                    filename = "fallback_activity.pdf"
                else:
                    return not_found_response("PDF for this activity")

            response = make_response(pdf_content)
            response.headers["Content-Type"] = "application/pdf"
            response.headers["Content-Disposition"] = f'inline; filename="{filename}"'
            return response

        except Exception as e:
            return error_response(f"Failed to retrieve PDF: {str(e)}", 500)
