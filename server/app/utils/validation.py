from __future__ import annotations

from typing import Any

from app.core.models import (
    ActivityFormat,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    PriorityCategory,
)
from app.db.models.activity import EnergyLevel


class ValidationError(Exception):
    """Custom exception for validation errors."""

    def __init__(self, message: str, field: str | None = None):
        self.message = message
        self.field = field
        super().__init__(message)


def _deduplicate_list(items: list) -> list:
    """Remove duplicates while preserving order."""
    seen = set()
    unique_items = []
    for item in items:
        if item not in seen:
            seen.add(item)
            unique_items.append(item)
    return unique_items


class RecommendationFormValidator:
    """Validator for teacher recommendation form input."""

    # Valid enum values
    VALID_FORMATS = [f.value for f in ActivityFormat]
    VALID_BLOOM_LEVELS = [b.value for b in BloomLevel]
    VALID_RESOURCES = [r.value for r in ActivityResource]
    VALID_TOPICS = [t.value for t in ActivityTopic]
    VALID_PRIORITY_CATEGORIES = [c.value for c in PriorityCategory]

    # Validation constraints
    MIN_AGE = 6
    MAX_AGE = 15
    MIN_DURATION = 5
    MAX_DURATION = 300  # 5 hours
    MIN_LESSON_PLAN_LENGTH = 2
    MAX_LESSON_PLAN_LENGTH = 5
    MAX_NAME_LENGTH = 100

    @classmethod
    def validate_recommendation_criteria(cls, criteria: dict[str, Any]) -> dict[str, Any]:
        """
        Validate and sanitize recommendation form criteria.

        Args:
            criteria: Raw criteria from the form

        Returns:
            Validated and sanitized criteria

        Raises:
            ValidationError: If validation fails
        """
        validated = {}

        # Validate name (optional)
        if "name" in criteria and criteria["name"] is not None:
            name = criteria["name"]
            if not isinstance(name, str):
                raise ValidationError("Name must be a string", "name")
            name = name.strip()
            if len(name) > cls.MAX_NAME_LENGTH:
                raise ValidationError(f"Name must be {cls.MAX_NAME_LENGTH} characters or less", "name")
            if name:  # Only add if not empty after strip
                validated["name"] = name

        # Validate target_age (optional)
        if "target_age" in criteria and criteria["target_age"] is not None:
            try:
                age = int(criteria["target_age"])
                if not (cls.MIN_AGE <= age <= cls.MAX_AGE):
                    raise ValidationError(f"Target age must be between {cls.MIN_AGE} and {cls.MAX_AGE}", "target_age")
                validated["target_age"] = age
            except (ValueError, TypeError) as e:
                raise ValidationError("Target age must be a valid integer", "target_age") from e

        # Validate format (optional array)
        if "format" in criteria and criteria["format"] is not None:
            formats = criteria["format"]
            if isinstance(formats, str):
                formats = [formats]
            elif not isinstance(formats, list):
                raise ValidationError("Format must be a string or array of strings", "format")

            validated_formats = []
            for fmt in formats:
                if not isinstance(fmt, str):
                    raise ValidationError("All format values must be strings", "format")
                fmt_lower = fmt.lower().strip()
                if fmt_lower not in cls.VALID_FORMATS:
                    raise ValidationError(f"Invalid format '{fmt}'. Must be one of: {cls.VALID_FORMATS}", "format")
                validated_formats.append(fmt_lower)

            if validated_formats:
                validated["format"] = _deduplicate_list(validated_formats)

        # Validate resources_needed (optional array)
        if "resources_needed" in criteria and criteria["resources_needed"] is not None:
            resources = criteria["resources_needed"]
            if isinstance(resources, str):
                resources = [resources]
            elif not isinstance(resources, list):
                raise ValidationError("Resources needed must be a string or array of strings", "resources_needed")

            validated_resources = []
            for resource in resources:
                if not isinstance(resource, str):
                    raise ValidationError("All resource values must be strings", "resources_needed")
                resource_lower = resource.lower().strip()
                if resource_lower not in cls.VALID_RESOURCES:
                    raise ValidationError(
                        f"Invalid resource '{resource}'. Must be one of: {cls.VALID_RESOURCES}", "resources_needed"
                    )
                validated_resources.append(resource_lower)

            if validated_resources:
                validated["resources_needed"] = _deduplicate_list(validated_resources)

        # Validate bloom_levels (optional array)
        if "bloom_levels" in criteria and criteria["bloom_levels"] is not None:
            bloom_levels = criteria["bloom_levels"]
            if isinstance(bloom_levels, str):
                bloom_levels = [bloom_levels]
            elif not isinstance(bloom_levels, list):
                raise ValidationError("Bloom levels must be a string or array of strings", "bloom_levels")

            validated_bloom_levels = []
            for bloom in bloom_levels:
                if not isinstance(bloom, str):
                    raise ValidationError("All bloom level values must be strings", "bloom_levels")
                bloom_lower = bloom.lower().strip()
                if bloom_lower not in cls.VALID_BLOOM_LEVELS:
                    raise ValidationError(
                        f"Invalid bloom level '{bloom}'. Must be one of: {cls.VALID_BLOOM_LEVELS}", "bloom_levels"
                    )
                validated_bloom_levels.append(bloom_lower)

            if validated_bloom_levels:
                validated["bloom_levels"] = _deduplicate_list(validated_bloom_levels)
            else:
                validated["bloom_levels"] = cls.VALID_BLOOM_LEVELS.copy()

        # Validate target_duration (optional)
        if "target_duration" in criteria and criteria["target_duration"] is not None:
            try:
                duration = int(criteria["target_duration"])
                if not (cls.MIN_DURATION <= duration <= cls.MAX_DURATION):
                    raise ValidationError(
                        f"Target duration must be between {cls.MIN_DURATION} and {cls.MAX_DURATION} minutes",
                        "target_duration",
                    )
                validated["target_duration"] = duration
            except (ValueError, TypeError) as e:
                raise ValidationError("Target duration must be a valid integer", "target_duration") from e

        # Validate topics (optional array)
        if "topics" in criteria and criteria["topics"] is not None:
            topics = criteria["topics"]
            if isinstance(topics, str):
                topics = [topics]
            elif not isinstance(topics, list):
                raise ValidationError("Topics must be a string or array of strings", "topics")

            validated_topics = []
            for topic in topics:
                if not isinstance(topic, str):
                    raise ValidationError("All topic values must be strings", "topics")
                topic_lower = topic.lower().strip()
                if topic_lower not in cls.VALID_TOPICS:
                    raise ValidationError(f"Invalid topic '{topic}'. Must be one of: {cls.VALID_TOPICS}", "topics")
                validated_topics.append(topic_lower)

            if validated_topics:
                validated["topics"] = _deduplicate_list(validated_topics)

        # Validate max_activity_count (optional)
        if "max_activity_count" in criteria and criteria["max_activity_count"] is not None:
            try:
                max_count = int(criteria["max_activity_count"])
                if not (1 <= max_count <= 10):
                    raise ValidationError(
                        "Max activity count must be between 1 and 10",
                        "max_activity_count",
                    )
                validated["max_activity_count"] = max_count
            except (ValueError, TypeError) as e:
                raise ValidationError("Max activity count must be a valid integer", "max_activity_count") from e

        # Validate priority_categories (optional array)
        if "priority_categories" in criteria and criteria["priority_categories"] is not None:
            priority_categories = criteria["priority_categories"]
            if isinstance(priority_categories, str):
                priority_categories = [priority_categories]
            elif not isinstance(priority_categories, list):
                raise ValidationError("Priority categories must be a string or array of strings", "priority_categories")

            validated_priority_categories = []
            for category in priority_categories:
                if not isinstance(category, str):
                    raise ValidationError("All priority category values must be strings", "priority_categories")
                category_lower = category.lower().strip()
                if category_lower not in cls.VALID_PRIORITY_CATEGORIES:
                    raise ValidationError(
                        f"Invalid priority category '{category}'. Must be one of: {cls.VALID_PRIORITY_CATEGORIES}",
                        "priority_categories",
                    )
                validated_priority_categories.append(category_lower)

            if validated_priority_categories:
                validated["priority_categories"] = _deduplicate_list(validated_priority_categories)

        # Validate limit (optional)
        if "limit" in criteria and criteria["limit"] is not None:
            try:
                limit = int(criteria["limit"])
                if not (1 <= limit <= 100):
                    raise ValidationError("Limit must be between 1 and 100", "limit")
                validated["limit"] = limit
            except (ValueError, TypeError) as e:
                raise ValidationError("Limit must be a valid integer", "limit") from e

        return validated

    @classmethod
    def validate_boolean_params(cls, params: dict[str, Any]) -> dict[str, bool]:
        """Validate boolean parameters."""
        validated = {}
        boolean_fields = [
            "allow_lesson_plans",
            "include_breaks",
            "save_search",
            "priority_age",
            "priority_topics",
            "priority_duration",
            "priority_bloom_level",
        ]

        for field in boolean_fields:
            if field in params and params[field] is not None:
                value = params[field]
                if isinstance(value, bool):
                    validated[field] = value
                elif isinstance(value, str):
                    validated[field] = value.lower() in ("true", "1", "yes", "on")
                else:
                    raise ValidationError(f"{field} must be a boolean value", field)

        return validated


class ActivityFormValidator:
    """Validator for admin activity creation form input."""

    # Validation constraints
    MIN_AGE = 6
    MAX_AGE = 15
    MIN_DURATION = 1
    MAX_DURATION = 300  # 5 hours
    MIN_PREP_CLEANUP = 0
    MAX_PREP_CLEANUP = 60  # 1 hour
    MAX_NAME_LENGTH = 255
    MAX_SOURCE_LENGTH = 255
    MAX_TOPIC_LENGTH = 50
    MAX_RESOURCE_LENGTH = 50

    @classmethod
    def validate_activity_data(cls, data: dict[str, Any]) -> dict[str, Any]:
        """
        Validate and sanitize activity creation form data.

        Args:
            data: Raw activity data from the form

        Returns:
            Validated and sanitized activity data

        Raises:
            ValidationError: If validation fails
        """
        validated = {}

        # Validate required fields
        required_fields = ["name", "age_min", "age_max", "format", "bloom_level", "duration_min_minutes"]
        for field in required_fields:
            if field not in data or data[field] is None:
                raise ValidationError(f"Required field '{field}' is missing", field)

        # Validate name (required)
        name = data["name"]
        if not isinstance(name, str):
            raise ValidationError("Name must be a string", "name")
        name = name.strip()
        if not name:
            raise ValidationError("Name cannot be empty", "name")
        if len(name) > cls.MAX_NAME_LENGTH:
            raise ValidationError(f"Name must be {cls.MAX_NAME_LENGTH} characters or less", "name")
        validated["name"] = name

        # Validate source (optional)
        if "source" in data and data["source"] is not None:
            source = data["source"]
            if not isinstance(source, str):
                raise ValidationError("Source must be a string", "source")
            source = source.strip()
            if len(source) > cls.MAX_SOURCE_LENGTH:
                raise ValidationError(f"Source must be {cls.MAX_SOURCE_LENGTH} characters or less", "source")
            if source:  # Only add if not empty after strip
                validated["source"] = source

        # Validate age_min (required)
        try:
            age_min = int(data["age_min"])
            if not (cls.MIN_AGE <= age_min <= cls.MAX_AGE):
                raise ValidationError(f"Age min must be between {cls.MIN_AGE} and {cls.MAX_AGE}", "age_min")
            validated["age_min"] = age_min
        except (ValueError, TypeError) as e:
            raise ValidationError("Age min must be a valid integer", "age_min") from e

        # Validate age_max (required)
        try:
            age_max = int(data["age_max"])
            if not (cls.MIN_AGE <= age_max <= cls.MAX_AGE):
                raise ValidationError(f"Age max must be between {cls.MIN_AGE} and {cls.MAX_AGE}", "age_max")
            if age_max < validated["age_min"]:
                raise ValidationError("Age max must be greater than or equal to age min", "age_max")
            validated["age_max"] = age_max
        except (ValueError, TypeError) as e:
            raise ValidationError("Age max must be a valid integer", "age_max") from e

        # Validate format (required)
        format_val = data["format"]
        if not isinstance(format_val, str):
            raise ValidationError("Format must be a string", "format")
        format_lower = format_val.lower().strip()
        valid_formats = [f.value for f in ActivityFormat]
        if format_lower not in valid_formats:
            raise ValidationError(f"Invalid format '{format_val}'. Must be one of: {valid_formats}", "format")
        validated["format"] = format_lower

        # Validate bloom_level (required)
        bloom = data["bloom_level"]
        if not isinstance(bloom, str):
            raise ValidationError("Bloom level must be a string", "bloom_level")
        bloom_lower = bloom.lower().strip()
        valid_bloom_levels = [b.value for b in BloomLevel]
        if bloom_lower not in valid_bloom_levels:
            raise ValidationError(f"Invalid bloom level '{bloom}'. Must be one of: {valid_bloom_levels}", "bloom_level")
        validated["bloom_level"] = bloom_lower

        # Validate duration_min_minutes (required)
        try:
            duration_min = int(data["duration_min_minutes"])
            if not (cls.MIN_DURATION <= duration_min <= cls.MAX_DURATION):
                raise ValidationError(
                    f"Duration min must be between {cls.MIN_DURATION} and {cls.MAX_DURATION} minutes",
                    "duration_min_minutes",
                )
            validated["duration_min_minutes"] = duration_min
        except (ValueError, TypeError) as e:
            raise ValidationError("Duration min must be a valid integer", "duration_min_minutes") from e

        # Validate duration_max_minutes (optional)
        if "duration_max_minutes" in data and data["duration_max_minutes"] is not None:
            try:
                duration_max = int(data["duration_max_minutes"])
                if not (cls.MIN_DURATION <= duration_max <= cls.MAX_DURATION):
                    raise ValidationError(
                        f"Duration max must be between {cls.MIN_DURATION} and {cls.MAX_DURATION} minutes",
                        "duration_max_minutes",
                    )
                if duration_max < validated["duration_min_minutes"]:
                    raise ValidationError(
                        "Duration max must be greater than or equal to duration min", "duration_max_minutes"
                    )
                validated["duration_max_minutes"] = duration_max
            except (ValueError, TypeError) as e:
                raise ValidationError("Duration max must be a valid integer", "duration_max_minutes") from e

        # Validate mental_load (optional)
        if "mental_load" in data and data["mental_load"] is not None:
            mental_load = data["mental_load"]
            if not isinstance(mental_load, str):
                raise ValidationError("Mental load must be a string", "mental_load")
            mental_load_lower = mental_load.lower().strip()
            valid_energy_levels = [e.value for e in EnergyLevel]
            if mental_load_lower not in valid_energy_levels:
                raise ValidationError(
                    f"Invalid mental load '{mental_load}'. Must be one of: {valid_energy_levels}", "mental_load"
                )
            validated["mental_load"] = mental_load_lower

        # Validate physical_energy (optional)
        if "physical_energy" in data and data["physical_energy"] is not None:
            physical_energy = data["physical_energy"]
            if not isinstance(physical_energy, str):
                raise ValidationError("Physical energy must be a string", "physical_energy")
            physical_energy_lower = physical_energy.lower().strip()
            valid_energy_levels = [e.value for e in EnergyLevel]
            if physical_energy_lower not in valid_energy_levels:
                raise ValidationError(
                    f"Invalid physical energy '{physical_energy}'. Must be one of: {valid_energy_levels}",
                    "physical_energy",
                )
            validated["physical_energy"] = physical_energy_lower

        # Validate prep_time_minutes (optional)
        if "prep_time_minutes" in data and data["prep_time_minutes"] is not None:
            try:
                prep_time = int(data["prep_time_minutes"])
                if not (cls.MIN_PREP_CLEANUP <= prep_time <= cls.MAX_PREP_CLEANUP):
                    raise ValidationError(
                        f"Prep time must be between {cls.MIN_PREP_CLEANUP} and {cls.MAX_PREP_CLEANUP} minutes",
                        "prep_time_minutes",
                    )
                if prep_time % 5 != 0:
                    raise ValidationError(
                        "Prep time must be in 5-minute increments (0, 5, 10, 15, etc.)",
                        "prep_time_minutes",
                    )
                validated["prep_time_minutes"] = prep_time
            except (ValueError, TypeError) as e:
                raise ValidationError("Prep time must be a valid integer", "prep_time_minutes") from e

        # Validate cleanup_time_minutes (optional)
        if "cleanup_time_minutes" in data and data["cleanup_time_minutes"] is not None:
            try:
                cleanup_time = int(data["cleanup_time_minutes"])
                if not (cls.MIN_PREP_CLEANUP <= cleanup_time <= cls.MAX_PREP_CLEANUP):
                    raise ValidationError(
                        f"Cleanup time must be between {cls.MIN_PREP_CLEANUP} and {cls.MAX_PREP_CLEANUP} minutes",
                        "cleanup_time_minutes",
                    )
                if cleanup_time % 5 != 0:
                    raise ValidationError(
                        "Cleanup time must be in 5-minute increments (0, 5, 10, 15, etc.)",
                        "cleanup_time_minutes",
                    )
                validated["cleanup_time_minutes"] = cleanup_time
            except (ValueError, TypeError) as e:
                raise ValidationError("Cleanup time must be a valid integer", "cleanup_time_minutes") from e

        # Validate resources_needed (optional array)
        if "resources_needed" in data and data["resources_needed"] is not None:
            resources = data["resources_needed"]
            if not isinstance(resources, list):
                raise ValidationError("Resources needed must be an array", "resources_needed")

            validated_resources = []
            for resource in resources:
                if not isinstance(resource, str):
                    raise ValidationError("All resource values must be strings", "resources_needed")
                resource = resource.strip()
                if not resource:
                    continue  # Skip empty strings
                if len(resource) > cls.MAX_RESOURCE_LENGTH:
                    raise ValidationError(
                        f"Resource name must be {cls.MAX_RESOURCE_LENGTH} characters or less", "resources_needed"
                    )
                validated_resources.append(resource)

            validated["resources_needed"] = validated_resources

        # Validate topics (optional array)
        if "topics" in data and data["topics"] is not None:
            topics = data["topics"]
            if not isinstance(topics, list):
                raise ValidationError("Topics must be an array", "topics")

            validated_topics = []
            for topic in topics:
                if not isinstance(topic, str):
                    raise ValidationError("All topic values must be strings", "topics")
                topic = topic.strip()
                if not topic:
                    continue  # Skip empty strings
                if len(topic) > cls.MAX_TOPIC_LENGTH:
                    raise ValidationError(f"Topic name must be {cls.MAX_TOPIC_LENGTH} characters or less", "topics")
                validated_topics.append(topic)

            validated["topics"] = validated_topics

        return validated


def validate_pagination_params(limit: int | None, offset: int | None) -> tuple[int, int]:
    """Validate pagination parameters with improved error messages."""
    # Validate limit
    if limit is not None:
        if not isinstance(limit, int):
            raise ValidationError("Limit must be an integer", "limit")
        if limit < 1:
            raise ValidationError("Limit must be at least 1", "limit")
        if limit > 100:
            raise ValidationError("Limit cannot exceed 100", "limit")
    else:
        limit = 50  # Default

    # Validate offset
    if offset is not None:
        if not isinstance(offset, int):
            raise ValidationError("Offset must be an integer", "offset")
        if offset < 0:
            raise ValidationError("Offset must be non-negative", "offset")
    else:
        offset = 0  # Default

    return limit, offset
