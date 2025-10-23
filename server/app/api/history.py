"""History API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask import request
from flask_openapi3 import Tag

from app.auth.decorators import auth_required
from app.db.database import get_db_session
from app.services.user_favourites_service import UserFavouritesService
from app.services.user_search_history_service import UserSearchHistoryService
from app.utils.pydantic_models import (
    ActivityFavouriteIdPath,
    ErrorResponse,
    FavouriteIdPath,
    HistoryIdPath,
    MessageResponse,
    SaveActivityFavouriteRequest,
    SaveLessonPlanFavouriteRequest,
)
from app.utils.response_helpers import error_response, success_response
from app.utils.validation import ValidationError, validate_pagination_params

# Define API tag for OpenAPI
history_tag = Tag(name="history", description="User history and favourites management")


def register_history_routes(api):
    """Register history routes with Flask-OpenAPI3."""

    @api.get(
        "/api/history/search",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 500: ErrorResponse},
        summary="Get search history",
        description="Get user's search history",
    )
    @auth_required
    def get_search_history():
        """Get search history."""
        try:
            user_id = request.user.id
            limit = request.args.get("limit", type=int)
            offset = request.args.get("offset", type=int)

            # Validate pagination parameters
            limit, offset = validate_pagination_params(limit, offset)

            search_history_service = UserSearchHistoryService(db=get_db_session())
            search_history = search_history_service.get_user_search_history(user_id=user_id, limit=limit, offset=offset)

            history_data = []
            for entry in search_history:
                history_data.append(
                    {
                        "id": entry.id,
                        "search_criteria": entry.search_criteria,
                        "created_at": entry.created_at.isoformat(),
                    }
                )

            return success_response(
                {
                    "search_history": history_data,
                    "pagination": {"limit": limit, "offset": offset, "count": len(history_data)},
                }
            )
        except ValidationError as e:
            return error_response(f"Validation error: {e.message}", 400)
        except Exception as e:
            return error_response(f"Failed to retrieve search history: {str(e)}", 500)

    @api.delete(
        "/api/history/search/<int:history_id>",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 404: ErrorResponse, 500: ErrorResponse},
        summary="Delete search history entry",
        description="Delete a specific search history entry",
    )
    @auth_required
    def delete_search_history_entry(path: HistoryIdPath):
        """Delete search history entry."""
        try:
            history_id = path.history_id
            user_id = request.user.id

            search_history_service = UserSearchHistoryService(db=get_db_session())
            success = search_history_service.delete_search_history(history_id=history_id, user_id=user_id)

            if not success:
                return error_response("Search history entry not found", 404)

            return success_response({"message": "Search history entry deleted successfully"})
        except Exception as e:
            return error_response(f"Failed to delete search history entry: {str(e)}", 500)

    @api.post(
        "/api/history/favourites/lesson-plans",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Save lesson plan favourite",
        description="Save a favourite lesson plan",
    )
    @auth_required
    def save_lesson_plan_favourite(body: SaveLessonPlanFavouriteRequest):
        """Save favourite lesson plan."""
        try:
            user_id = request.user.id
            name = body.name
            activity_ids = body.activity_ids
            lesson_plan_snapshot = body.lesson_plan

            favourites_service = UserFavouritesService(db=get_db_session())
            favourite = favourites_service.save_lesson_plan_favourite(
                user_id=user_id,
                activity_ids=activity_ids,
                name=name,
                lesson_plan_snapshot=lesson_plan_snapshot,
            )

            return success_response(
                {
                    "message": "Lesson plan favourite saved successfully",
                    "favourite_id": favourite.id,
                }
            )
        except Exception as e:
            return error_response(f"Failed to save lesson plan favourite: {str(e)}", 500)

    @api.get(
        "/api/history/favourites/lesson-plans",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 500: ErrorResponse},
        summary="Get lesson plan favourites",
        description="Get user's favourite lesson plans",
    )
    @auth_required
    def get_lesson_plan_favourites():
        """Get favourite lesson plans."""
        try:
            user_id = request.user.id
            limit = request.args.get("limit", type=int)
            offset = request.args.get("offset", type=int)

            # Validate pagination parameters
            limit, offset = validate_pagination_params(limit, offset)

            favourites_service = UserFavouritesService(db=get_db_session())
            favourites = favourites_service.get_user_lesson_plan_favourites(user_id=user_id, limit=limit, offset=offset)

            favourites_data = []
            for favourite in favourites:
                favourites_data.append(
                    {
                        "id": favourite.id,
                        "favourite_type": favourite.favourite_type,
                        "name": favourite.name,
                        "activity_ids": favourite.activity_ids or [],
                        "lesson_plan": favourite.lesson_plan_snapshot,
                        "created_at": favourite.created_at.isoformat(),
                    }
                )

            return success_response(
                {
                    "favourites": favourites_data,
                    "pagination": {"limit": limit, "offset": offset, "count": len(favourites_data)},
                }
            )
        except ValidationError as e:
            return error_response(f"Validation error: {e.message}", 400)
        except Exception as e:
            return error_response(f"Failed to retrieve lesson plan favourites: {str(e)}", 500)

    @api.delete(
        "/api/history/favourites/<int:favourite_id>",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 404: ErrorResponse, 500: ErrorResponse},
        summary="Delete favourite",
        description="Delete a specific favourite lesson plan",
    )
    @auth_required
    def delete_favourite(path: FavouriteIdPath):
        """Delete favourite lesson plan."""
        try:
            favourite_id = path.favourite_id
            user_id = request.user.id

            favourites_service = UserFavouritesService(db=get_db_session())
            success = favourites_service.delete_favourite(favourite_id=favourite_id, user_id=user_id)

            if not success:
                return error_response("Favourite not found", 404)

            return success_response({"message": "Favourite deleted successfully"})
        except Exception as e:
            return error_response(f"Failed to delete favourite: {str(e)}", 500)

    # Individual Activity Favourites Endpoints

    @api.post(
        "/api/history/favourites/activities",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 422: ErrorResponse, 500: ErrorResponse},
        summary="Save activity favourite",
        description="Save an individual activity as favourite",
    )
    @auth_required
    def save_activity_favourite(body: SaveActivityFavouriteRequest):
        """Save individual activity as favourite."""
        try:
            user_id = request.user.id
            activity_id = body.activity_id
            name = body.name

            favourites_service = UserFavouritesService(db=get_db_session())

            # Check if already favourited
            if favourites_service.is_activity_favourited(user_id, activity_id):
                return error_response("Activity is already in favourites", 409)

            favourite = favourites_service.save_activity_favourite(
                user_id=user_id,
                activity_id=activity_id,
                name=name,
            )

            return success_response(
                {
                    "message": "Activity favourite saved successfully",
                    "favourite_id": favourite.id,
                }
            )
        except Exception as e:
            return error_response(f"Failed to save activity favourite: {str(e)}", 500)

    @api.get(
        "/api/history/favourites/activities",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 500: ErrorResponse},
        summary="Get activity favourites",
        description="Get user's favourite activities",
    )
    @auth_required
    def get_activity_favourites():
        """Get favourite activities."""
        try:
            user_id = request.user.id
            limit = request.args.get("limit", type=int)
            offset = request.args.get("offset", type=int)

            # Validate pagination parameters
            limit, offset = validate_pagination_params(limit, offset)

            favourites_service = UserFavouritesService(db=get_db_session())
            favourites = favourites_service.get_user_activity_favourites(user_id=user_id, limit=limit, offset=offset)

            favourites_data = []
            for favourite in favourites:
                favourites_data.append(
                    {
                        "id": favourite.id,
                        "favourite_type": favourite.favourite_type,
                        "activity_id": favourite.activity_id,
                        "name": favourite.name,
                        "created_at": favourite.created_at.isoformat(),
                    }
                )

            return success_response(
                {
                    "favourites": favourites_data,
                    "pagination": {"limit": limit, "offset": offset, "count": len(favourites_data)},
                }
            )
        except ValidationError as e:
            return error_response(f"Validation error: {e.message}", 400)
        except Exception as e:
            return error_response(f"Failed to retrieve activity favourites: {str(e)}", 500)

    @api.delete(
        "/api/history/favourites/activities/<int:activity_id>",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 404: ErrorResponse, 500: ErrorResponse},
        summary="Remove activity favourite",
        description="Remove an activity from favourites",
    )
    @auth_required
    def remove_activity_favourite(path: ActivityFavouriteIdPath):
        """Remove activity from favourites."""
        try:
            activity_id = path.activity_id
            user_id = request.user.id

            favourites_service = UserFavouritesService(db=get_db_session())
            success = favourites_service.remove_activity_favourite(user_id=user_id, activity_id=activity_id)

            if not success:
                return error_response("Activity favourite not found", 404)

            return success_response({"message": "Activity favourite removed successfully"})
        except Exception as e:
            return error_response(f"Failed to remove activity favourite: {str(e)}", 500)

    @api.get(
        "/api/history/favourites/activities/<int:activity_id>/status",
        tags=[history_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 500: ErrorResponse},
        summary="Check activity favourite status",
        description="Check if an activity is favourited by the user",
    )
    @auth_required
    def check_activity_favourite_status(path: ActivityFavouriteIdPath):
        """Check if activity is favourited."""
        try:
            activity_id = path.activity_id
            user_id = request.user.id

            favourites_service = UserFavouritesService(db=get_db_session())
            is_favourited = favourites_service.is_activity_favourited(user_id=user_id, activity_id=activity_id)

            return success_response({"is_favourited": is_favourited})
        except Exception as e:
            return error_response(f"Failed to check activity favourite status: {str(e)}", 500)
