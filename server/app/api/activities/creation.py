"""Activity creation API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask import request
from flask_openapi3 import Tag

from app.auth.decorators import admin_required
from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.services.pdf_processor import PDFProcessor
from app.services.pdf_service import PDFService
from app.utils.pydantic_models import ActivityCreationRequest, ErrorResponse, MessageResponse
from app.utils.response_helpers import error_response, success_response
from app.utils.validation import ValidationError

# Define API tag for OpenAPI
activities_tag = Tag(name="activities", description="Activity management and recommendations")


def register_activities_creation_routes(api):
    """Register activity creation routes with Flask-OpenAPI3."""

    @api.post(
        "/api/activities/upload-and-create",
        tags=[activities_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Upload PDF and create activity",
        description="Upload PDF, extract data, and create activity in one step",
    )
    @admin_required
    def upload_and_create_activity():
        """Upload PDF, process it, and create activity automatically."""
        try:
            # Validate file upload
            if "pdf_file" not in request.files:
                return error_response("No PDF file part", 400)

            pdf_file = request.files["pdf_file"]
            if pdf_file.filename == "":
                return error_response("No selected PDF file", 400)

            if not pdf_file.filename.lower().endswith(".pdf"):
                return error_response("File must be a PDF", 400)

            pdf_content = pdf_file.read()
            if len(pdf_content) == 0:
                return error_response("PDF file is empty", 400)

            # Store PDF
            pdf_service = PDFService()
            document_id = pdf_service.store_pdf(pdf_content, pdf_file.filename)

            # Process PDF to extract activity data
            pdf_processor = PDFProcessor()
            result = pdf_processor.parse_pdf_content(pdf_content, document_id)

            if "error" in result:
                return error_response(f"Failed to process PDF: {result['error']}", 400)

            # Update PDF document with extraction results
            confidence_score = None
            if result.get("confidence") is not None:
                confidence_score = f"{result['confidence']:.3f}"

            pdf_service.update_pdf_extraction_results(
                document_id=document_id,
                extracted_fields=result.get("data"),
                confidence_score=confidence_score,
                extraction_quality=result.get("extraction_quality"),
            )

            # Create activity with extracted data
            extracted_data = result.get("data", {})
            activity_data = {
                "name": extracted_data.get("name", ""),
                "description": extracted_data.get("description", ""),
                "source": extracted_data.get("source", ""),
                "age_min": extracted_data.get("age_min", 6),  # Default to 6
                "age_max": extracted_data.get("age_max", 12),  # Default to 12
                "format": extracted_data.get("format", "unplugged"),
                "bloom_level": extracted_data.get("bloom_level", "remember"),
                "duration_min_minutes": extracted_data.get("duration_min_minutes", 15),
                "duration_max_minutes": extracted_data.get("duration_max_minutes"),
                "mental_load": extracted_data.get("mental_load", "medium"),
                "physical_energy": extracted_data.get("physical_energy", "medium"),
                "prep_time_minutes": extracted_data.get("prep_time_minutes", 5),
                "cleanup_time_minutes": extracted_data.get("cleanup_time_minutes", 5),
                "resources_needed": extracted_data.get("resources_needed", []),
                "topics": extracted_data.get("topics", []),
                "document_id": document_id,
            }

            # Validate activity data
            try:
                validated_data = ActivityCreationRequest(**activity_data)
            except Exception as e:
                return error_response(f"Invalid extracted data: {str(e)}", 400)

            # Create activity
            new_activity = Activity(**validated_data.model_dump())
            get_db_session().add(new_activity)
            get_db_session().commit()

            return success_response(
                {
                    "activity": new_activity.to_dict(),
                    "document_id": document_id,
                    "extraction_confidence": result.get("confidence"),
                    "extraction_quality": result.get("extraction_quality"),
                },
                201,
            )

        except Exception as e:
            get_db_session().rollback()
            return error_response(f"Failed to upload and create activity: {str(e)}", 500)

    @api.post(
        "/api/activities/create",
        tags=[activities_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Create activity",
        description="Create a new activity from PDF document",
    )
    @admin_required
    def create_activity(body: ActivityCreationRequest):
        """Create new activity."""
        try:
            # Validate that the document_id exists
            document_id = body.document_id
            if not document_id:
                return error_response("document_id is required", 400)

            # Check if document exists in filesystem
            if not isinstance(document_id, int) or document_id <= 0:
                return error_response("Invalid document_id", 400)

            pdf_service = PDFService()
            # Check if the actual PDF file exists
            pdf_content = pdf_service.get_pdf_content(document_id)
            if pdf_content is None:
                return error_response(f"PDF document with ID {document_id} does not exist", 400)

            # Create activity with validated data
            activity_data = body.model_dump()
            new_activity = Activity(**activity_data)
            get_db_session().add(new_activity)
            get_db_session().commit()

            return success_response(
                {
                    "activity": new_activity.to_dict(),
                },
                201,
            )
        except ValidationError as e:
            return error_response(f"Validation error: {e.message}", 400)
        except Exception as e:
            get_db_session().rollback()
            return error_response(f"Failed to create activity: {str(e)}", 500)
