"""Lesson plan API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask import make_response
from flask_openapi3 import Tag

from app.utils.pydantic_models import (
    ErrorResponse,
    LessonPlanInfoRequest,
    LessonPlanInfoResponse,
    LessonPlanRequest,
    MessageResponse,
)
from app.utils.response_helpers import error_response

# Define API tag for OpenAPI
activities_tag = Tag(name="activities", description="Activity management and recommendations")


def _get_pdf_service():
    """Get PDF service instance."""
    from app.services.pdf_service import PDFService

    return PDFService()


def register_activities_lesson_plan_routes(api):
    """Register lesson plan routes with Flask-OpenAPI3."""

    @api.post(
        "/api/activities/lesson-plan",
        tags=[activities_tag],
        responses={200: MessageResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Generate lesson plan",
        description="Generate PDF lesson plan from selected activities. Returns application/pdf as an attachment.",
    )
    def generate_lesson_plan(body: LessonPlanRequest):
        """Generate lesson plan PDF."""
        try:
            activities = body.activities
            search_criteria = body.search_criteria
            total_duration = body.total_duration

            # Extract breaks from activities (they're embedded in break_after field)
            # or use breaks provided directly in the request
            breaks = []
            if hasattr(body, "breaks") and body.breaks:
                # Use breaks provided directly in the request
                breaks = body.breaks
            else:
                # Fallback: extract breaks from activities' break_after field, but never from the last activity
                for idx, activity in enumerate(activities):
                    if idx < len(activities) - 1 and activity.get("break_after"):
                        breaks.append(activity["break_after"])

            # SAFEGUARD: Never allow a break after the final activity.
            # There can be at most (len(activities) - 1) breaks between activities.
            max_breaks = max(len(activities) - 1, 0)
            if len(breaks) > max_breaks:
                breaks = breaks[:max_breaks]

            pdf_service = _get_pdf_service()
            info = pdf_service.get_lesson_plan_info(activities)
            if not info["can_generate_lesson_plan"]:
                return error_response("No PDFs available for the selected activities")

            lesson_plan_pdf = pdf_service.generate_lesson_plan(
                activities=activities,
                search_criteria=search_criteria,
                breaks=breaks,
                total_duration=total_duration,
            )

            response = make_response(lesson_plan_pdf)
            response.headers["Content-Type"] = "application/pdf"
            response.headers["Content-Disposition"] = 'attachment; filename="lesson_plan.pdf"'
            return response
        except Exception as e:
            return error_response(f"Failed to generate lesson plan: {str(e)}", 500)

    @api.post(
        "/api/activities/lesson-plan/info",
        tags=[activities_tag],
        responses={200: LessonPlanInfoResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Get lesson plan info",
        description="Get lesson plan generation information",
    )
    def get_lesson_plan_info(body: LessonPlanInfoRequest):
        """Get lesson plan info."""
        try:
            activities = body.activities

            pdf_service = _get_pdf_service()
            info = pdf_service.get_lesson_plan_info(activities)

            return info
        except Exception as e:
            return error_response(f"Failed to get lesson plan info: {str(e)}", 500)
