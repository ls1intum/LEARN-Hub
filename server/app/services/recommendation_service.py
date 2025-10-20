from __future__ import annotations

from typing import Any

from sqlalchemy.orm import Session

from app.api.schemas import RecommendationItemResponse
from app.core.engine import get_recommendations
from app.core.models import (
    ActivityFormat,
    ActivityModel,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    EnergyLevel,
    PriorityCategory,
    SearchCriteria,
)
from app.db.models.activity import Activity


class RecommendationService:
    """Optimized recommendation service with improved error handling and performance."""

    def __init__(self, db: Session):
        self.db = db

    def _convert_criteria_to_typed(self, criteria: dict[str, Any]) -> tuple[SearchCriteria, list[PriorityCategory]]:
        """Convert dict criteria to typed SearchCriteria and PriorityCategory list with validation."""
        # Extract priority categories with better error handling
        priority_categories = self._extract_priority_categories(criteria.get("priority_categories", []))

        # Convert criteria to SearchCriteria with validation
        search_criteria = SearchCriteria(
            name=criteria.get("name"),
            target_age=self._convert_to_int(criteria.get("target_age")),
            format=self._convert_format_list(criteria.get("format")),
            bloom_levels=self._convert_bloom_levels_list(criteria.get("bloom_levels")),
            target_duration=self._convert_to_int(criteria.get("target_duration")),
            available_resources=self._convert_resources_list(criteria.get("available_resources")),
            preferred_topics=self._convert_topics_list(criteria.get("preferred_topics")),
        )

        return search_criteria, priority_categories

    def _extract_priority_categories(self, priority_values: list[str] | None) -> list[PriorityCategory]:
        """Extract and validate priority categories with improved error handling."""
        if not priority_values or not isinstance(priority_values, list):
            return []

        priority_categories = []
        for category_value in priority_values:
            try:
                priority_categories.append(PriorityCategory(category_value))
            except ValueError:
                # Log invalid priority category but continue processing
                continue

        return priority_categories

    def _convert_to_int(self, value: Any) -> int | None:
        """Convert value to integer with validation."""
        if value is None:
            return None
        try:
            return int(value)
        except (ValueError, TypeError):
            return None

    def _convert_format_list(self, formats: Any) -> list[ActivityFormat] | None:
        """Convert format list to ActivityFormat enums with validation."""
        if not formats:
            return None

        if isinstance(formats, str):
            formats = [formats]

        if not isinstance(formats, list):
            return None

        try:
            return [ActivityFormat(f) for f in formats if f and isinstance(f, str)]
        except ValueError:
            return None

    def _convert_bloom_levels_list(self, bloom_levels: Any) -> list[BloomLevel] | None:
        """Convert bloom levels list to BloomLevel enums with validation."""
        if not bloom_levels:
            return None

        if isinstance(bloom_levels, str):
            bloom_levels = [bloom_levels]

        if not isinstance(bloom_levels, list):
            return None

        try:
            return [BloomLevel(b) for b in bloom_levels if b and isinstance(b, str)]
        except ValueError:
            return None

    def _convert_resources_list(self, resources: Any) -> list[ActivityResource] | None:
        """Convert resources list to ActivityResource enums with validation."""
        if not resources:
            return None

        if isinstance(resources, str):
            resources = [resources]

        if not isinstance(resources, list):
            return None

        try:
            return [ActivityResource(r) for r in resources if r and isinstance(r, str)]
        except ValueError:
            return None

    def _convert_topics_list(self, topics: Any) -> list[ActivityTopic] | None:
        """Convert topics list to ActivityTopic enums with validation."""
        if not topics:
            return None

        if isinstance(topics, str):
            topics = [topics]

        if not isinstance(topics, list):
            return None

        try:
            return [ActivityTopic(t) for t in topics if t and isinstance(t, str)]
        except ValueError:
            return None

    def get_recommendations(
        self,
        criteria: dict[str, Any],
        include_breaks: bool = False,
        max_activity_count: int = 2,
        limit: int = 10,
    ) -> dict[str, Any]:
        """
        Enhanced recommendation service that returns a structured response.

        Args:
            criteria: Search criteria including optional priority constraints
            include_breaks: Whether to calculate and include break information
            max_activity_count: Maximum number of activities per lesson plan
            limit: Maximum number of lesson plans to return

        Returns:
            Dictionary with recommendations data directly
        """
        try:
            # Convert dict criteria to typed objects with validation
            search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)

            # Get all activities from database and convert to models efficiently
            activity_models = self._load_and_convert_activities()

            # Get recommendations from engine using typed interface
            recommendations = get_recommendations(
                criteria=search_criteria,
                activities=activity_models,
                priority_categories=priority_categories,
                include_breaks=include_breaks,
                max_activity_count=max_activity_count,
                limit=limit,
            )

            # Convert core engine output to API response format
            # Create individual recommendation responses
            recommendation_items = []
            for activities, score in recommendations:
                # Convert ActivityModel to dictionary format
                activity_responses = []
                for activity in activities:
                    activity_data = {
                        "id": activity.id,
                        "name": activity.name,
                        "description": activity.description,
                        "source": activity.source,
                        "age_min": activity.age_min,
                        "age_max": activity.age_max,
                        "format": activity.format.value,
                        "resources_needed": [r.value for r in activity.resources_needed],
                        "bloom_level": activity.bloom_level.value,
                        "duration_min_minutes": activity.duration_min_minutes,
                        "duration_max_minutes": activity.duration_max_minutes,
                        "topics": [t.value for t in activity.topics],
                        "mental_load": activity.mental_load.value if activity.mental_load else None,
                        "physical_energy": activity.physical_energy.value if activity.physical_energy else None,
                        "prep_time_minutes": activity.prep_time_minutes,
                        "cleanup_time_minutes": activity.cleanup_time_minutes,
                        "pdf_path": activity.pdf_path,
                        "pdf_uploaded_at": activity.pdf_uploaded_at,
                        "type": "activity",
                    }
                    activity_responses.append(activity_data)

                # Calculate total duration
                total_duration = sum(activity.duration_min_minutes for activity in activities)

                recommendation_items.append(
                    RecommendationItemResponse(
                        activities=activity_responses,
                        score=score.total_score,
                        total_duration_minutes=total_duration,
                        is_lesson_plan=len(activities) > 1,
                    )
                )

            # Return data directly
            return {
                "recommendations": [item.model_dump() for item in recommendation_items],
                "total_count": len(recommendation_items),
                "search_criteria": criteria.model_dump() if hasattr(criteria, "model_dump") else criteria,
            }

        except Exception as e:
            # Log error and return empty response
            print(f"DEBUG: Error in get_recommendations: {e}")
            import traceback

            traceback.print_exc()
            return {
                "recommendations": [],
                "total_count": 0,
                "search_criteria": criteria.model_dump() if hasattr(criteria, "model_dump") else criteria,
            }

    def _load_and_convert_activities(self) -> list[ActivityModel]:
        """Load activities from database and convert to ActivityModel objects efficiently."""
        activities = self.db.query(Activity).all()
        activity_models = []

        for activity in activities:
            try:
                activity_model = self._convert_db_activity_to_model(activity)
                activity_models.append(activity_model)
            except Exception:
                # Skip invalid activities but continue processing
                continue

        return activity_models

    def _convert_db_activity_to_model(self, activity: Activity) -> ActivityModel:
        """Convert a database Activity to ActivityModel with proper enum conversion."""
        # Coerce scalar enums from DB (strings or SQLAlchemy Enums) into domain enums
        format_enum = (
            activity.format if isinstance(activity.format, ActivityFormat) else ActivityFormat(activity.format)
        )
        bloom_enum = (
            activity.bloom_level if isinstance(activity.bloom_level, BloomLevel) else BloomLevel(activity.bloom_level)
        )
        mental_enum = activity.mental_load if isinstance(activity.mental_load, EnergyLevel) else activity.mental_load
        physical_enum = (
            activity.physical_energy if isinstance(activity.physical_energy, EnergyLevel) else activity.physical_energy
        )

        # Coerce list fields into enum lists (DB stores JSON strings)
        raw_resources = activity.resources_needed or []
        resources_list = [r if isinstance(r, ActivityResource) else ActivityResource(r) for r in raw_resources]
        raw_topics = activity.topics or []
        topics_list = [t if isinstance(t, ActivityTopic) else ActivityTopic(t) for t in raw_topics]

        return ActivityModel(
            id=activity.id,
            name=activity.name,
            description=activity.description,
            source=activity.source,
            age_min=activity.age_min,
            age_max=activity.age_max,
            format=format_enum,
            bloom_level=bloom_enum,
            duration_min_minutes=activity.duration_min_minutes,
            duration_max_minutes=activity.duration_max_minutes,
            mental_load=mental_enum,
            physical_energy=physical_enum,
            prep_time_minutes=activity.prep_time_minutes,
            cleanup_time_minutes=activity.cleanup_time_minutes,
            resources_needed=resources_list,
            topics=topics_list,
        )
