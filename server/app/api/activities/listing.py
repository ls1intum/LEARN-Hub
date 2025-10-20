"""Activity listing API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask_openapi3 import Tag
from sqlalchemy import or_

from app.auth.decorators import admin_required
from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.utils.pydantic_models import (
    ActivityIdPath,
    ActivityListData,
    ActivityListRequest,
    ActivityResponse,
    ErrorResponse,
    MessageResponse,
)
from app.utils.response_helpers import error_response, success_response

# Define API tag for OpenAPI
activities_tag = Tag(name="activities", description="Activity management and recommendations")


def register_activities_listing_routes(api):
    """Register activity listing routes with Flask-OpenAPI3."""

    @api.get(
        "/api/activities/",
        tags=[activities_tag],
        responses={200: ActivityListData, 500: ErrorResponse},
        summary="Get activities",
        description="Get a list of activities with optional filtering and pagination",
    )
    def get_activities(query: ActivityListRequest):
        """Get a list of activities."""
        try:
            # Query parameters are already validated by Flask-OpenAPI3
            name_filter = query.name
            age_min_filter = query.age_min
            age_max_filter = query.age_max
            format_filters = query.format
            bloom_level_filters = query.bloom_level
            resources_filters = query.resources_needed
            topics_filters = query.topics

            # Pagination
            limit = query.limit
            offset = query.offset

            db_session = get_db_session()
            db_query = db_session.query(Activity)

            # Apply filters
            if name_filter:
                db_query = db_query.filter(Activity.name.ilike(f"%{name_filter}%"))

            if age_min_filter is not None:
                db_query = db_query.filter(Activity.age_min >= age_min_filter)

            if age_max_filter is not None:
                db_query = db_query.filter(Activity.age_max <= age_max_filter)

            if format_filters:
                db_query = db_query.filter(Activity.format.in_(format_filters))

            if bloom_level_filters:
                db_query = db_query.filter(Activity.bloom_level.in_(bloom_level_filters))

            if resources_filters:
                # Filter activities that have any of the specified resources
                resource_conditions = []
                for resource in resources_filters:
                    resource_conditions.append(Activity.resources_needed.contains([resource]))
                db_query = db_query.filter(or_(*resource_conditions))

            if topics_filters:
                # Filter activities that have any of the specified topics
                topic_conditions = []
                for topic in topics_filters:
                    topic_conditions.append(Activity.topics.contains([topic]))
                db_query = db_query.filter(or_(*topic_conditions))

            # Get total count before pagination
            total_count = db_query.count()

            # Apply pagination
            activities = db_query.offset(offset).limit(limit).all()

            # Convert to dict format
            activities_data = []
            for activity in activities:
                # Helper function to get enum value or string
                def get_enum_value(field):
                    return field.value if hasattr(field, "value") else field

                activity_dict = {
                    "id": activity.id,
                    "name": activity.name,
                    "description": activity.description,
                    "source": activity.source,
                    "age_min": activity.age_min,
                    "age_max": activity.age_max,
                    "duration_min_minutes": activity.duration_min_minutes,
                    "duration_max_minutes": activity.duration_max_minutes,
                    "format": get_enum_value(activity.format),
                    "bloom_level": get_enum_value(activity.bloom_level),
                    "resources_needed": [get_enum_value(r) for r in activity.resources_needed],
                    "topics": [get_enum_value(t) for t in activity.topics],
                    "mental_load": get_enum_value(activity.mental_load),
                    "physical_energy": get_enum_value(activity.physical_energy),
                    "prep_time_minutes": activity.prep_time_minutes,
                    "cleanup_time_minutes": activity.cleanup_time_minutes,
                    "document_id": activity.document_id,
                    "type": "activity",
                }
                activities_data.append(activity_dict)

            return success_response(
                {
                    "activities": activities_data,
                    "total": total_count,
                    "limit": limit,
                    "offset": offset,
                }
            )

        except Exception as e:
            return error_response(f"Failed to retrieve activities: {str(e)}", 500)

    @api.get(
        "/api/activities/<int:activity_id>",
        tags=[activities_tag],
        responses={
            200: ActivityResponse,
            404: ErrorResponse,
            500: ErrorResponse,
        },
        summary="Get activity by ID",
        description="Get a specific activity by its ID",
    )
    def get_activity(path: ActivityIdPath):
        """Get specific activity by ID."""
        try:
            activity_id = path.activity_id
            db_session = get_db_session()
            activity = db_session.query(Activity).filter(Activity.id == activity_id).first()

            if not activity:
                return error_response("Activity not found", 404)

            # Helper function to get enum value or string
            def get_enum_value(field):
                return field.value if hasattr(field, "value") else field

            activity_dict = {
                "id": activity.id,
                "name": activity.name,
                "description": activity.description,
                "source": activity.source,
                "age_min": activity.age_min,
                "age_max": activity.age_max,
                "duration_min_minutes": activity.duration_min_minutes,
                "duration_max_minutes": activity.duration_max_minutes,
                "format": get_enum_value(activity.format),
                "bloom_level": get_enum_value(activity.bloom_level),
                "resources_needed": [get_enum_value(r) for r in activity.resources_needed],
                "topics": [get_enum_value(t) for t in activity.topics],
                "mental_load": get_enum_value(activity.mental_load),
                "physical_energy": get_enum_value(activity.physical_energy),
                "prep_time_minutes": activity.prep_time_minutes,
                "cleanup_time_minutes": activity.cleanup_time_minutes,
                "document_id": activity.document_id,
                "type": "activity",
            }

            return success_response(activity_dict)

        except Exception as e:
            return error_response(f"Failed to retrieve activity: {str(e)}", 500)

    @api.delete(
        "/api/activities/<int:activity_id>",
        tags=[activities_tag],
        responses={
            200: MessageResponse,
            401: ErrorResponse,
            403: ErrorResponse,
            404: ErrorResponse,
            500: ErrorResponse,
        },
        summary="Delete activity",
        description="Delete a specific activity by ID (admin only)",
    )
    @admin_required
    def delete_activity(path: ActivityIdPath):
        """Delete specific activity by ID."""
        try:
            activity_id = path.activity_id
            db_session = get_db_session()
            activity = db_session.query(Activity).filter(Activity.id == activity_id).first()

            if not activity:
                return error_response("Activity not found", 404)

            db_session.delete(activity)
            db_session.commit()

            return success_response({"message": "Activity deleted successfully"})

        except Exception as e:
            db_session.rollback()
            return error_response(f"Failed to delete activity: {str(e)}", 500)
