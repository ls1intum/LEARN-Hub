"""API schemas for request/response models and OpenAPI documentation."""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel

from app.utils.pydantic_models import ActivityResponse

# ============================================================================
# RESPONSE MODELS
# ============================================================================


class UserResponse(BaseModel):
    """User response model."""

    id: int
    email: str
    first_name: str
    last_name: str
    role: str


# Removed old wrapped response models - using direct response pattern from response_helpers.py


class BreakResponse(BaseModel):
    """Break response model."""

    type: str = "break"
    duration: int
    description: str
    reasons: list[str]


class ScoreResponse(BaseModel):
    """Score response model."""

    total_score: int
    category_scores: dict[str, dict[str, Any]]
    priority_categories: list[str]
    is_sequence: bool = False
    activity_count: int = 1


class RecommendationItemResponse(BaseModel):
    """Response model for individual recommendation items."""

    activities: list[dict[str, Any] | BreakResponse]
    score: int
    total_duration_minutes: int | None = None
    is_lesson_plan: bool = False


# Removed old wrapped response models - using direct response pattern from response_helpers.py


# Removed old wrapped response models - using direct response pattern from response_helpers.py


class LessonPlanResponse(BaseModel):
    """Lesson plan response model."""

    id: int | None = None
    name: str
    description: str | None = None
    activities: list[ActivityResponse | BreakResponse]
    total_duration_minutes: int
    created_at: str | None = None
    updated_at: str | None = None


class DocumentResponse(BaseModel):
    """Document response model."""

    id: int
    filename: str
    original_filename: str
    file_size: int
    uploaded_at: str
    file_path: str | None = None


class SearchHistoryEntryResponse(BaseModel):
    """Search history entry response model."""

    id: int
    search_criteria: dict[str, Any]
    created_at: str


class SearchHistoryResponse(BaseModel):
    """Search history response model."""

    search_history: list[SearchHistoryEntryResponse]
    pagination: dict[str, int]


# Removed old wrapped response models - using direct response pattern from response_helpers.py


class FavoriteResponse(BaseModel):
    """Favorite response model."""

    id: int
    user_id: int
    activity_ids: list[int]
    search_criteria: dict[str, Any]
    ordering_method: str
    total_duration: int
    created_at: str


class FavoritesResponse(BaseModel):
    """Favorites response model."""

    favourites: list[FavoriteResponse]
    pagination: dict[str, int]


class LessonPlanInfoResponse(BaseModel):
    """Lesson plan info response model."""

    can_generate_lesson_plan: bool
    available_pdfs: int
    total_activities: int
    missing_pdfs: list[str]


# ============================================================================
# COMMON PARAMETERS (for reference, not used in Flask-OpenAPI3)
# ============================================================================

# These are kept for reference but Flask-OpenAPI3 generates parameters automatically
# from Pydantic models and function signatures

# ============================================================================
# SCHEMA EXPORTS (for backward compatibility)
# ============================================================================

# Flask-OpenAPI3 automatically generates schemas from Pydantic models
# These are kept for any remaining references in the codebase
RESPONSE_SCHEMAS = {}
REQUEST_SCHEMAS = {}

# Removed old schema definitions - using direct response pattern from response_helpers.py
