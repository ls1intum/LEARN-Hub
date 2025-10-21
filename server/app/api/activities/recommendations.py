"""Activity recommendations API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from datetime import datetime

from flask import request
from flask_openapi3 import Tag
from pydantic import ValidationError

from app.auth.decorators import maybe_auth
from app.core.constants import SCORING_CATEGORIES
from app.db.database import get_db_session
from app.services.user_search_history_service import UserSearchHistoryService
from app.utils.pydantic_models import (
    ActivityResponse,
    BreakResponse,
    CategoryScoreResponse,
    ErrorResponse,
    RecommendationRequest,
    RecommendationsResponse,
    ScoringInsightsResponse,
)
from app.utils.response_helpers import error_response, success_response

# Define API tag for OpenAPI
activities_tag = Tag(name="activities", description="Activity management and recommendations")


def register_activities_recommendations_routes(api):
    """Register activity recommendations routes with Flask-OpenAPI3."""

    @api.get(
        "/api/activities/recommendations",
        tags=[activities_tag],
        responses={200: RecommendationsResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Get recommendations",
        description="Get activity recommendations based on criteria",
    )
    @maybe_auth
    def get_recommendations():
        """Get activity recommendations."""
        try:
            # Create Pydantic model with elegant parameter parsing
            query = RecommendationRequest.from_flask_request(request)
            include_breaks = query.include_breaks
            limit = query.limit
            max_activity_count = query.max_activity_count

            criteria_tuple = query.to_engine_criteria()
            search_criteria, priority_categories = criteria_tuple
            user_id = getattr(request, "user", None)
            if user_id and hasattr(user_id, "id"):
                search_history_service = UserSearchHistoryService(db=get_db_session())
                search_history_service.save_search_query(
                    user_id=user_id.id, search_criteria=request.args.to_dict(flat=True)
                )

            from app.core.engine import get_recommendations as core_get_recommendations
            from app.services.recommendation_service import RecommendationService

            db = get_db_session()
            recommendation_service = RecommendationService(db)
            activity_models = recommendation_service._load_and_convert_activities()

            recommendations = core_get_recommendations(
                criteria=search_criteria,
                activities=activity_models,
                priority_categories=priority_categories,
                include_breaks=include_breaks,
                max_activity_count=max_activity_count,
                limit=limit,
            )

            recommendations_data = []
            for activities_list, score in recommendations:
                activity_responses = []
                for index, activity in enumerate(activities_list):

                    def get_enum_value(field):
                        return field.value if hasattr(field, "value") else field

                    break_after_data = None
                    # Never expose a break after the final activity in API responses
                    is_last = index == len(activities_list) - 1
                    if not is_last and hasattr(activity, "break_after") and activity.break_after:
                        break_after_data = BreakResponse(
                            id=f"break-{activity.id}",
                            duration=activity.break_after.duration,
                            description=activity.break_after.description,
                            reasons=activity.break_after.reasons,
                        )

                    activity_data = ActivityResponse(
                        id=activity.id,
                        name=activity.name,
                        description=activity.description,
                        source=activity.source,
                        age_min=activity.age_min,
                        age_max=activity.age_max,
                        format=get_enum_value(activity.format),
                        bloom_level=get_enum_value(activity.bloom_level),
                        duration_min_minutes=activity.duration_min_minutes,
                        duration_max_minutes=activity.duration_max_minutes,
                        mental_load=get_enum_value(activity.mental_load) if activity.mental_load else None,
                        physical_energy=(
                            get_enum_value(activity.physical_energy) if activity.physical_energy else None
                        ),
                        prep_time_minutes=activity.prep_time_minutes,
                        cleanup_time_minutes=activity.cleanup_time_minutes,
                        resources_needed=(
                            [get_enum_value(r) for r in activity.resources_needed] if activity.resources_needed else []
                        ),
                        topics=[get_enum_value(t) for t in activity.topics] if activity.topics else [],
                        document_id=getattr(activity, "document_id", None),
                        type="activity",
                        break_after=break_after_data,
                    )

                    activity_responses.append(activity_data)

                # Convert category scores to response format
                score_breakdown = {}
                for category, category_score in score.category_scores.items():
                    score_breakdown[category] = CategoryScoreResponse(
                        category=category_score.category,
                        score=category_score.score,
                        impact=category_score.impact,
                        priority_multiplier=category_score.priority_multiplier,
                        is_priority=category_score.is_priority,
                    )

                # Create recommendation entry
                recommendation_entry = {
                    "activities": [activity.model_dump() for activity in activity_responses],
                    "score": score.total_score,
                    "score_breakdown": {k: v.model_dump() for k, v in score_breakdown.items()},
                }
                recommendations_data.append(recommendation_entry)

            response_data = {
                "activities": recommendations_data,
                "total": len(recommendations_data),
                "search_criteria": request.args.to_dict(flat=True),
                "generated_at": datetime.now(),
            }
            return success_response(response_data)

        except ValidationError as e:
            return error_response(f"Validation error: {str(e)}", 422)
        except Exception as e:
            return error_response(f"Failed to get recommendations: {str(e)}", 500)

    @api.get(
        "/api/activities/scoring-insights",
        tags=[activities_tag],
        responses={200: ScoringInsightsResponse, 500: ErrorResponse},
        summary="Get scoring insights",
        description="Get scoring insights for activities",
    )
    def get_scoring_insights():
        """Get scoring insights."""
        try:
            # Return the scoring categories and their descriptions
            insights = {
                "categories": SCORING_CATEGORIES,
                "description": "Scoring categories used to evaluate activity recommendations",
            }
            return success_response(insights)

        except Exception as e:
            return error_response(f"Failed to get scoring insights: {str(e)}", 500)
