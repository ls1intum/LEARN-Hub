"""Document management API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask import make_response, request
from flask_openapi3 import Tag

from app.auth.decorators import admin_required
from app.services.pdf_processor import PDFProcessor
from app.services.pdf_service import PDFService
from app.utils.pydantic_models import DocumentIdPath, DocumentInfoResponse, ErrorResponse, MessageResponse
from app.utils.response_helpers import (
    error_response,
    not_found_response,
    success_response,
)

# Define API tag for OpenAPI
documents_tag = Tag(name="documents", description="Document management")


def register_documents_routes(api):
    """Register document routes with Flask-OpenAPI3."""

    @api.post(
        "/api/documents/upload_pdf",
        tags=[documents_tag],
        responses={200: MessageResponse, 400: ErrorResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Upload PDF",
        description="Upload and process PDF document for activity creation",
    )
    @admin_required
    def upload_pdf():
        """Upload PDF document."""
        try:
            if "pdf_file" not in request.files:
                return error_response("No PDF file part")

            pdf_file = request.files["pdf_file"]
            if pdf_file.filename == "":
                return error_response("No selected PDF file")

            if not pdf_file.filename.lower().endswith(".pdf"):
                return error_response("File must be a PDF")

            pdf_content = pdf_file.read()
            if len(pdf_content) == 0:
                return error_response("PDF file is empty")

            # Store PDF using minimal service
            pdf_service = PDFService()
            document_id = pdf_service.store_pdf(pdf_content, pdf_file.filename)

            return success_response(
                {
                    "document_id": document_id,
                    "filename": pdf_file.filename,
                    "file_size": len(pdf_content),
                },
                201,
            )

        except Exception as e:
            return error_response(f"Failed to upload PDF: {str(e)}", 500)

    @api.get(
        "/api/documents/<int:document_id>",
        tags=[documents_tag],
        responses={200: {}, 404: ErrorResponse, 500: ErrorResponse},
        summary="Get document",
        description="Retrieve PDF file content by document ID",
    )
    def get_document(path: DocumentIdPath):
        """Get PDF document."""
        try:
            document_id = path.document_id
            pdf_service = PDFService()
            pdf_content = pdf_service.get_pdf_content(document_id)

            if pdf_content is None:
                return not_found_response("PDF")

            pdf_info = pdf_service.get_pdf_info(document_id)
            filename = pdf_info["filename"] if pdf_info else f"document_{document_id}.pdf"

            response = make_response(pdf_content)
            response.headers["Content-Type"] = "application/pdf"
            response.headers["Content-Disposition"] = f'inline; filename="{filename}"'
            return response

        except Exception as e:
            return error_response(f"Failed to get document: {str(e)}", 500)

    @api.get(
        "/api/documents/<int:document_id>/info",
        tags=[documents_tag],
        responses={200: DocumentInfoResponse, 404: ErrorResponse, 500: ErrorResponse},
        summary="Get document info",
        description="Get PDF document information and metadata",
    )
    def get_document_info(path: DocumentIdPath):
        """Get document information."""
        try:
            document_id = path.document_id
            pdf_service = PDFService()
            pdf_info = pdf_service.get_pdf_info(document_id)

            if pdf_info is None:
                return not_found_response("PDF document")

            return success_response(pdf_info)

        except Exception as e:
            return error_response(f"Failed to get document info: {str(e)}", 500)

    @api.post(
        "/api/documents/<int:document_id>/process",
        tags=[documents_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 404: ErrorResponse, 500: ErrorResponse},
        summary="Process PDF document",
        description="Extract activity data from PDF document using LLM",
    )
    @admin_required
    def process_pdf(path: DocumentIdPath):
        """Process PDF document and extract activity data."""
        try:
            document_id = path.document_id

            # Get PDF content
            pdf_service = PDFService()
            pdf_content = pdf_service.get_pdf_content(document_id)

            if pdf_content is None:
                return not_found_response("PDF document")

            # Process PDF with LLM
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

            return success_response(
                {
                    "document_id": document_id,
                    "extracted_data": result.get("data"),
                    "confidence": result.get("confidence"),
                    "text_length": result.get("text_length"),
                    "extraction_quality": result.get("extraction_quality"),
                }
            )

        except Exception as e:
            return error_response(f"Failed to process PDF: {str(e)}", 500)
