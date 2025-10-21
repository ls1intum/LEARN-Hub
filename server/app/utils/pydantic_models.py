"""Pydantic models for request/response validation."""

from __future__ import annotations

from datetime import datetime

# Import for type hints
from typing import TYPE_CHECKING, Any

from pydantic import BaseModel, EmailStr, Field, field_validator

if TYPE_CHECKING:
    from app.core.models import PriorityCategory


class LoginRequest(BaseModel):
    """Login request validation for admin and teacher password authentication."""

    email: EmailStr = Field(..., description="User's email address")
    password: str = Field(..., min_length=1, max_length=255, description="User's password")

    @field_validator("password")
    @classmethod
    def validate_password(cls, v: str) -> str:
        """Validate password is not empty after stripping."""
        if not v.strip():
            raise ValueError("Password cannot be empty")
        return v.strip()


class RefreshTokenRequest(BaseModel):
    """Refresh token request validation."""

    refresh_token: str = Field(..., min_length=1, max_length=500)

    @field_validator("refresh_token")
    @classmethod
    def validate_refresh_token(cls, v: str) -> str:
        """Validate refresh token is not empty after stripping."""
        if not v.strip():
            raise ValueError("Refresh token cannot be empty")
        return v.strip()


class VerificationCodeRequest(BaseModel):
    """Request a 6-digit verification code for teacher login."""

    email: EmailStr = Field(..., description="Teacher's email address")


class VerifyCodeRequest(BaseModel):
    """Verify 6-digit code and complete teacher login."""

    email: EmailStr = Field(..., description="Teacher's email address")
    code: str = Field(..., min_length=6, max_length=6, pattern=r"^\d{6}$", description="6-digit verification code")

    @field_validator("code")
    @classmethod
    def validate_code(cls, v: str) -> str:
        """Validate code is 6 digits."""
        if not v.isdigit():
            raise ValueError("Code must contain only digits")
        return v


class TeacherRegistrationRequest(BaseModel):
    """Teacher auto-registration request validation."""

    email: EmailStr
    first_name: str = Field(..., min_length=1, max_length=100)
    last_name: str = Field(..., min_length=1, max_length=100)


class PasswordResetRequest(BaseModel):
    """Password reset request validation."""

    email: EmailStr


class CreateUserRequest(BaseModel):
    """Create user request validation."""

    email: EmailStr
    first_name: str = Field(..., min_length=1, max_length=100)
    last_name: str = Field(..., min_length=1, max_length=100)
    role: str = Field(..., pattern=r"^(TEACHER|ADMIN)$")
    password: str | None = Field(None, min_length=8, max_length=255)

    @field_validator("password")
    @classmethod
    def validate_password_for_admin(cls, v: str | None, info) -> str | None:
        """Password is required for ADMIN users. TEACHER users get auto-generated passwords."""
        role = info.data.get("role")

        if role == "ADMIN" and not v:
            raise ValueError("Password is required for ADMIN users")
        if v and not v.strip():
            raise ValueError("Password cannot be empty")
        return v.strip() if v else None


class UpdateUserRequest(BaseModel):
    """Update user request validation."""

    email: EmailStr | None = None
    first_name: str | None = Field(None, min_length=1, max_length=100)
    last_name: str | None = Field(None, min_length=1, max_length=100)
    role: str | None = Field(None, pattern=r"^(TEACHER|ADMIN)$")
    password: str | None = Field(None, min_length=8, max_length=255)

    @field_validator("password")
    @classmethod
    def validate_password(cls, v: str | None, info) -> str | None:
        """Validate password if provided."""
        if v and not v.strip():
            raise ValueError("Password cannot be empty")
        return v.strip() if v else None


class ActivityCreationRequest(BaseModel):
    """Activity creation request validation."""

    name: str = Field(..., min_length=1, max_length=255)
    description: str = Field(..., min_length=25, max_length=1000)
    source: str | None = Field(None, max_length=255)
    age_min: int = Field(..., ge=6, le=15)
    age_max: int = Field(..., ge=6, le=15)
    format: str = Field(..., pattern=r"^(digital|hybrid|unplugged)$")
    bloom_level: str = Field(..., pattern=r"^(remember|understand|apply|analyze|evaluate|create)$")
    duration_min_minutes: int = Field(..., ge=1, le=300)
    duration_max_minutes: int | None = Field(None, ge=1, le=300)
    mental_load: str | None = Field(None, pattern=r"^(low|medium|high)$")
    physical_energy: str | None = Field(None, pattern=r"^(low|medium|high)$")
    prep_time_minutes: int | None = Field(None, ge=0, le=60)
    cleanup_time_minutes: int | None = Field(None, ge=0, le=60)
    resources_needed: list[str] | None = None
    topics: list[str] | None = None
    document_id: int = Field(..., ge=1, description="ID of the uploaded PDF document")

    @field_validator("age_max")
    @classmethod
    def validate_age_max(cls, v: int, info) -> int:
        """Age max must be >= age min."""
        if "age_min" in info.data and v < info.data["age_min"]:
            raise ValueError("Age max must be greater than or equal to age min")
        return v

    @field_validator("prep_time_minutes")
    @classmethod
    def validate_prep_time(cls, v: int | None) -> int | None:
        """Prep time must be in 5-minute increments."""
        if v is not None and v % 5 != 0:
            raise ValueError("Prep time must be in 5-minute increments (0, 5, 10, 15, etc.)")
        return v

    @field_validator("cleanup_time_minutes")
    @classmethod
    def validate_cleanup_time(cls, v: int | None) -> int | None:
        """Cleanup time must be in 5-minute increments."""
        if v is not None and v % 5 != 0:
            raise ValueError("Cleanup time must be in 5-minute increments (0, 5, 10, 15, etc.)")
        return v

    @field_validator("duration_max_minutes")
    @classmethod
    def validate_duration_max(cls, v: int | None, info) -> int | None:
        """Duration max must be >= duration min."""
        if v is not None and "duration_min_minutes" in info.data and v < info.data["duration_min_minutes"]:
            raise ValueError("Duration max must be greater than or equal to duration min")
        return v

    @field_validator("resources_needed")
    @classmethod
    def validate_resources(cls, v: list[str] | None) -> list[str] | None:
        """Validate and clean resources list."""
        if v is None:
            return None
        cleaned = []
        for resource in v:
            if isinstance(resource, str) and resource.strip():
                if len(resource.strip()) > 50:
                    raise ValueError("Resource name must be 50 characters or less")
                cleaned.append(resource.strip())
        return cleaned

    @field_validator("topics")
    @classmethod
    def validate_topics(cls, v: list[str] | None) -> list[str] | None:
        """Validate and clean topics list."""
        if v is None:
            return None
        cleaned = []
        for topic in v:
            if isinstance(topic, str) and topic.strip():
                if len(topic.strip()) > 50:
                    raise ValueError("Topic name must be 50 characters or less")
                cleaned.append(topic.strip())
        return cleaned

    @field_validator("name", "source")
    @classmethod
    def validate_string_fields(cls, v: str | None) -> str | None:
        """Validate and clean string fields."""
        if v is not None and not v.strip():
            return None
        return v.strip() if v else None


class LessonPlanRequest(BaseModel):
    """Lesson plan generation request validation."""

    activities: list[dict[str, Any]] = Field(
        ..., min_length=1, description="List of ActivityModels with embedded breaks to include in lesson plan"
    )
    search_criteria: dict[str, Any] = Field(..., description="Search criteria used to find activities")
    total_duration: int = Field(default=0, ge=0, le=480, description="Total duration in minutes (0-480)")
    breaks: list[dict[str, Any]] | None = Field(None, description="Optional list of breaks to include in lesson plan")


class LessonPlanInfoRequest(BaseModel):
    """Lesson plan info request validation."""

    activities: list[dict[str, Any]] = Field(
        ..., min_length=1, description="List of ActivityModels with embedded breaks to check"
    )


class SaveActivityFavouriteRequest(BaseModel):
    """Save individual activity favourite request validation."""

    activity_id: int = Field(..., gt=0, description="ID of the activity to favourite")
    name: str | None = Field(None, description="Optional custom name for the favourite")


class SaveLessonPlanFavouriteRequest(BaseModel):
    """Save lesson plan favourite request validation."""

    name: str | None = Field(None, description="Optional custom name for the favourite lesson plan")
    activity_ids: list[int] = Field(..., min_length=1, description="List of activity IDs to save as favourite")
    # Snapshot of the full lesson plan payload at time of favouriting (required going forward)
    lesson_plan: dict[str, Any] = Field(
        ..., description="Snapshot of lesson plan (activities with inline breaks, metadata)"
    )


# (Legacy SaveFavoriteRequest removed)


# Response Models


class UserResponse(BaseModel):
    """User response model."""

    id: int
    email: str
    first_name: str
    last_name: str
    role: str
    is_verified: bool
    created_at: datetime


class BreakResponse(BaseModel):
    """Break response model - represents a break between activities."""

    type: str = Field(default="break", description="Type identifier for the break")
    id: str = Field(..., description="Unique identifier for the break")
    duration: int = Field(..., ge=1, description="Duration of the break in minutes")
    description: str = Field(..., description="Description of the break")
    reasons: list[str] = Field(..., description="List of reasons why this break was necessary")


class ActivityResponse(BaseModel):
    """Activity response model - returns activity data directly."""

    id: int
    name: str
    description: str
    source: str | None = None
    age_min: int
    age_max: int
    duration_min_minutes: int
    duration_max_minutes: int | None = None
    format: str
    bloom_level: str
    resources_needed: list[str] = []
    topics: list[str] = []
    mental_load: str | None = None
    physical_energy: str | None = None
    prep_time_minutes: int | None = None
    cleanup_time_minutes: int | None = None
    document_id: int | None = None
    type: str = "activity"
    break_after: BreakResponse | None = Field(None, description="Break that should happen after this activity")


class RecommendationItemResponse(BaseModel):
    """Recommendation item response model."""

    activity: ActivityResponse
    score: float
    reasoning: str | None = None
    category_scores: dict[str, float] | None = None


class CategoryScoreResponse(BaseModel):
    """Category score response model - represents detailed scoring information for a category."""

    category: str
    score: int
    impact: int
    priority_multiplier: float
    is_priority: bool


class RecommendationEntryResponse(BaseModel):
    """Recommendation entry response model - represents a single recommendation with activities and scoring."""

    activities: list[ActivityResponse]
    score: float
    score_breakdown: dict[str, CategoryScoreResponse]


class RecommendationsResponse(BaseModel):
    """Recommendations response model - returns recommendations data directly."""

    activities: list[RecommendationEntryResponse]
    total: int
    search_criteria: dict[str, Any]
    generated_at: datetime


class DocumentResponse(BaseModel):
    """Document response model."""

    id: int
    filename: str
    file_size: int
    mime_type: str | None = None
    created_at: datetime


class DocumentInfoResponse(BaseModel):
    """Document info response model - returns document info directly."""

    id: int
    filename: str
    file_size: int
    mime_type: str | None = None
    created_at: datetime


class SearchHistoryEntryResponse(BaseModel):
    """Search history entry response model."""

    id: int
    search_criteria: dict[str, Any]
    results_count: int
    created_at: datetime


class ActivityFavouriteResponse(BaseModel):
    """Individual activity favourite response model."""

    id: int
    favourite_type: str = "activity"
    activity_id: int
    name: str | None = None
    created_at: datetime


class LessonPlanFavouriteResponse(BaseModel):
    """Lesson plan favourite response model."""

    id: int
    favourite_type: str = "lesson_plan"
    activity_ids: list[int]
    name: str | None = None
    created_at: datetime


# (Legacy FavoriteResponse removed)


class LessonPlanInfoResponse(BaseModel):
    """Lesson plan info response model."""

    title: str
    total_duration: int
    activity_count: int
    topics_covered: list[str]
    bloom_levels: list[str]
    age_range: str
    formats: list[str]


class ScoringInsightsResponse(BaseModel):
    """Scoring insights response model - returns insights data directly."""

    insights: dict[str, Any]
    generated_at: datetime


class FieldValuesResponse(BaseModel):
    """Field values response model - returns field values directly."""

    format: list[str]
    resources_available: list[str]
    bloom_level: list[str]
    topics: list[str]
    mental_load: list[str]
    physical_energy: list[str]
    priority_categories: list[str]


# Standard API Response Wrappers


class ErrorResponse(BaseModel):
    """Standard error response wrapper."""

    error: str


class MessageResponse(BaseModel):
    """Simple message response."""

    message: str


class TokenResponse(BaseModel):
    """Token response model."""

    access_token: str
    refresh_token: str
    expires_in: int


class LoginSuccessData(BaseModel):
    """Login success response data with user info and JWT tokens."""

    user: UserResponse = Field(..., description="User information")
    access_token: str = Field(..., description="JWT access token for API authentication")
    refresh_token: str = Field(..., description="JWT refresh token for token renewal")
    expires_in: int = Field(..., description="Access token expiration time in seconds")


class ActivityListData(BaseModel):
    """Activity list response data - returns activities data directly."""

    activities: list[ActivityResponse]
    total: int
    limit: int
    offset: int


class UserListData(BaseModel):
    """User list response data."""

    users: list[UserResponse]
    total: int


# Request Models for Recommendations


class RecommendationRequest(BaseModel):
    """Recommendation request validation."""

    name: str | None = None
    target_age: int | None = Field(None, ge=6, le=15)
    format: list[str] | None = None
    bloom_levels: list[str] | None = None
    target_duration: int | None = Field(None, ge=1, le=480)
    available_resources: list[str] | None = None
    preferred_topics: list[str] | None = None
    priority_categories: list[str] | None = None
    include_breaks: bool = False
    limit: int = Field(10, ge=1, le=50)
    max_activity_count: int = Field(2, ge=1, le=10)

    @classmethod
    def from_flask_request(cls, request) -> RecommendationRequest:
        """Create RecommendationRequest from Flask request with elegant parameter parsing."""
        from flask import request

        # Get all query parameters (preserving multiple values)
        raw_params = request.args.to_dict(flat=False)

        # Process list-type parameters elegantly
        list_fields = {"format", "bloom_levels", "available_resources", "preferred_topics", "priority_categories"}
        processed_params = {}

        for key, value in raw_params.items():
            if key in list_fields:
                # Flatten and clean all values (handles both comma-separated and multiple params)
                all_values = []
                for item in (value if isinstance(value, list) else [value]):
                    if item and "," in item:
                        all_values.extend([v.strip() for v in item.split(",") if v.strip()])
                    elif item:
                        all_values.append(item.strip())
                processed_params[key] = all_values if all_values else None
            else:
                # Single values - take first if list
                processed_params[key] = value[0] if isinstance(value, list) and value else value

        return cls(**processed_params)

    @field_validator("format")
    @classmethod
    def validate_format(cls, v: list[str] | None) -> list[str] | None:
        """Validate format values."""
        if v is None:
            return None
        valid_formats = ["digital", "hybrid", "unplugged"]
        for fmt in v:
            if fmt not in valid_formats:
                raise ValueError(f"Invalid format: {fmt}. Must be one of {valid_formats}")
        return v

    @field_validator("bloom_levels")
    @classmethod
    def validate_bloom_levels(cls, v: list[str] | None) -> list[str] | None:
        """Validate bloom level values."""
        if v is None:
            return None
        valid_levels = ["remember", "understand", "apply", "analyze", "evaluate", "create"]
        for level in v:
            if level not in valid_levels:
                raise ValueError(f"Invalid bloom level: {level}. Must be one of {valid_levels}")
        return v

    @field_validator("available_resources")
    @classmethod
    def validate_available_resources(cls, v: list[str] | None) -> list[str] | None:
        """Validate available resources values."""
        if v is None:
            return None
        valid_resources = ["computers", "tablets", "handouts", "blocks", "electronics", "stationery"]
        for resource in v:
            if resource not in valid_resources:
                raise ValueError(f"Invalid resource: {resource}. Must be one of {valid_resources}")
        return v

    @field_validator("preferred_topics")
    @classmethod
    def validate_preferred_topics(cls, v: list[str] | None) -> list[str] | None:
        """Validate preferred topics values."""
        if v is None:
            return None
        valid_topics = ["decomposition", "patterns", "abstraction", "algorithms"]
        for topic in v:
            if topic not in valid_topics:
                raise ValueError(f"Invalid topic: {topic}. Must be one of {valid_topics}")
        return v

    def to_engine_criteria(self) -> tuple[Any, list[PriorityCategory]]:
        """Convert to engine criteria with simple validation."""
        from app.core.models import (
            ActivityFormat,
            ActivityResource,
            ActivityTopic,
            BloomLevel,
            PriorityCategory,
            SearchCriteria,
        )

        # Convert priority categories
        priority_categories = []
        if self.priority_categories:
            for cat in self.priority_categories:
                try:
                    priority_categories.append(PriorityCategory(cat))
                except ValueError:
                    # Skip invalid priority categories
                    continue

        # Convert other fields with simple validation
        search_criteria = SearchCriteria(
            name=self.name,
            target_age=self.target_age,
            format=[ActivityFormat(f) for f in self.format] if self.format else None,
            bloom_levels=[BloomLevel(b) for b in self.bloom_levels] if self.bloom_levels else None,
            target_duration=self.target_duration,
            available_resources=(
                [ActivityResource(r) for r in self.available_resources] if self.available_resources else None
            ),
            preferred_topics=([ActivityTopic(t) for t in self.preferred_topics] if self.preferred_topics else None),
        )

        return search_criteria, priority_categories


# Path Parameter Models


class ActivityIdPath(BaseModel):
    """Path parameter for activity ID."""

    activity_id: int = Field(..., ge=1, description="Activity ID")


class DocumentIdPath(BaseModel):
    """Path parameter for document ID."""

    document_id: int = Field(..., ge=1, description="Document ID")


class HistoryIdPath(BaseModel):
    """Path parameter for history ID."""

    history_id: int = Field(..., ge=1, description="History entry ID")


class FavouriteIdPath(BaseModel):
    """Path parameter for favourite ID."""

    favourite_id: int = Field(..., ge=1, description="Favourite ID")


class ActivityFavouriteIdPath(BaseModel):
    """Path parameter for activity favourite ID."""

    activity_id: int = Field(..., ge=1, description="Activity ID")


class UserIdPath(BaseModel):
    """Path parameter for user ID."""

    user_id: int = Field(..., ge=1, description="User ID")


class ActivityListRequest(BaseModel):
    """Activity list request with filtering and pagination."""

    name: str | None = Field(None, description="Filter by activity name (partial match)")
    age_min: int | None = Field(None, ge=6, le=15, description="Minimum age filter")
    age_max: int | None = Field(None, ge=6, le=15, description="Maximum age filter")
    format: list[str] | str | None = Field(None, description="Filter by activity formats")
    bloom_level: list[str] | str | None = Field(None, description="Filter by Bloom's taxonomy levels")
    resources_needed: list[str] | str | None = Field(None, description="Filter by required resources")
    topics: list[str] | str | None = Field(None, description="Filter by CS topics")
    limit: int = Field(10, ge=1, le=100, description="Number of activities to return")
    offset: int = Field(0, ge=0, description="Number of activities to skip")

    @field_validator("format", "bloom_level", "resources_needed", "topics", mode="before")
    @classmethod
    def convert_to_list(cls, v):
        """Convert single strings to lists for consistency."""
        if v is None:
            return None
        if isinstance(v, str):
            return [v]
        return v

    @field_validator("format")
    @classmethod
    def validate_format(cls, v: list[str] | None) -> list[str] | None:
        """Validate format values."""
        if v is None:
            return None
        valid_formats = ["digital", "hybrid", "unplugged"]
        for fmt in v:
            if fmt not in valid_formats:
                raise ValueError(f"Invalid format: {fmt}. Must be one of {valid_formats}")
        return v

    @field_validator("bloom_level")
    @classmethod
    def validate_bloom_levels(cls, v: list[str] | None) -> list[str] | None:
        """Validate bloom level values."""
        if v is None:
            return None
        valid_levels = ["remember", "understand", "apply", "analyze", "evaluate", "create"]
        for level in v:
            if level not in valid_levels:
                raise ValueError(f"Invalid bloom level: {level}. Must be one of {valid_levels}")
        return v
