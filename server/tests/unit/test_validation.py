"""
Validation tests for app/utils/validation.py.
"""

import pytest

from app.utils.validation import (
    ActivityFormValidator,
    RecommendationFormValidator,
    ValidationError,
    _deduplicate_list,
    validate_pagination_params,
)


class TestDeduplicateList:
    """Test the _deduplicate_list utility function."""

    def test_deduplicate_preserves_order(self):
        """Test that deduplication preserves order of first occurrence."""
        items = ["a", "b", "a", "c", "b", "d"]
        result = _deduplicate_list(items)
        assert result == ["a", "b", "c", "d"]

    def test_deduplicate_empty_list(self):
        """Test deduplication of empty list."""
        assert _deduplicate_list([]) == []

    def test_deduplicate_no_duplicates(self):
        """Test deduplication when no duplicates exist."""
        items = ["a", "b", "c"]
        assert _deduplicate_list(items) == items

    def test_deduplicate_all_same(self):
        """Test deduplication when all items are the same."""
        items = ["a", "a", "a"]
        assert _deduplicate_list(items) == ["a"]


class TestValidationError:
    """Test ValidationError exception."""

    def test_validation_error_message_only(self):
        """Test ValidationError with message only."""
        error = ValidationError("Test error")
        assert str(error) == "Test error"
        assert error.field is None

    def test_validation_error_with_field(self):
        """Test ValidationError with message and field."""
        error = ValidationError("Test error", "test_field")
        assert str(error) == "Test error"
        assert error.field == "test_field"


class TestRecommendationFormValidator:
    """Test RecommendationFormValidator - comprehensive coverage."""

    def test_validate_empty_criteria(self):
        """Test validation with empty criteria."""
        result = RecommendationFormValidator.validate_recommendation_criteria({})
        assert result == {}

    def test_validate_name_valid(self):
        """Test name validation with valid input."""
        criteria = {"name": "Test Activity"}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["name"] == "Test Activity"

    def test_validate_name_whitespace_trim(self):
        """Test name validation trims whitespace."""
        criteria = {"name": "  Test Activity  "}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["name"] == "Test Activity"

    def test_validate_name_empty_after_trim(self):
        """Test name validation with empty string after trim."""
        criteria = {"name": "   "}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert "name" not in result

    def test_validate_name_too_long(self):
        """Test name validation with too long name."""
        long_name = "a" * 101
        criteria = {"name": long_name}
        with pytest.raises(ValidationError, match="Name must be 100 characters or less"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_name_invalid_type(self):
        """Test name validation with invalid type."""
        criteria = {"name": 123}
        with pytest.raises(ValidationError, match="Name must be a string"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_target_age_valid(self):
        """Test target age validation with valid input."""
        criteria = {"target_age": 10}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["target_age"] == 10

    def test_validate_target_age_string_conversion(self):
        """Test target age validation with string input."""
        criteria = {"target_age": "12"}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["target_age"] == 12

    def test_validate_target_age_too_low(self):
        """Test target age validation with age too low."""
        criteria = {"target_age": 5}
        with pytest.raises(ValidationError, match="Target age must be between 6 and 15"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_target_age_too_high(self):
        """Test target age validation with age too high."""
        criteria = {"target_age": 16}
        with pytest.raises(ValidationError, match="Target age must be between 6 and 15"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_target_age_invalid_type(self):
        """Test target age validation with invalid type."""
        criteria = {"target_age": "invalid"}
        with pytest.raises(ValidationError, match="Target age must be a valid integer"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_format_string(self):
        """Test format validation with string input."""
        criteria = {"format": "digital"}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["format"] == ["digital"]

    def test_validate_format_list(self):
        """Test format validation with list input."""
        criteria = {"format": ["digital", "unplugged"]}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["format"] == ["digital", "unplugged"]

    def test_validate_format_case_insensitive(self):
        """Test format validation is case insensitive."""
        criteria = {"format": "DIGITAL"}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["format"] == ["digital"]

    def test_validate_format_whitespace_trim(self):
        """Test format validation trims whitespace."""
        criteria = {"format": "  digital  "}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["format"] == ["digital"]

    def test_validate_format_deduplication(self):
        """Test format validation removes duplicates."""
        criteria = {"format": ["digital", "digital", "unplugged"]}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["format"] == ["digital", "unplugged"]

    def test_validate_format_invalid(self):
        """Test format validation with invalid format."""
        criteria = {"format": "invalid"}
        with pytest.raises(ValidationError, match="Invalid format"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_format_invalid_type(self):
        """Test format validation with invalid type."""
        criteria = {"format": 123}
        with pytest.raises(ValidationError, match="Format must be a string or array of strings"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_format_empty_list(self):
        """Test format validation with empty list."""
        criteria = {"format": []}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert "format" not in result

    def test_validate_resources_needed_valid(self):
        """Test resources needed validation with valid input."""
        criteria = {"resources_needed": ["computers", "tablets"]}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["resources_needed"] == ["computers", "tablets"]

    def test_validate_resources_needed_string(self):
        """Test resources needed validation with string input."""
        criteria = {"resources_needed": "computers"}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["resources_needed"] == ["computers"]

    def test_validate_resources_needed_invalid(self):
        """Test resources needed validation with invalid resource."""
        criteria = {"resources_needed": ["invalid_resource"]}
        with pytest.raises(ValidationError, match="Invalid resource"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_bloom_levels_valid(self):
        """Test bloom levels validation with valid input."""
        criteria = {"bloom_levels": ["understand", "apply"]}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["bloom_levels"] == ["understand", "apply"]

    def test_validate_bloom_levels_empty_defaults(self):
        """Test bloom levels defaults to all levels when empty."""
        criteria = {"bloom_levels": []}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["bloom_levels"] == RecommendationFormValidator.VALID_BLOOM_LEVELS.copy()

    def test_validate_bloom_levels_invalid(self):
        """Test bloom levels validation with invalid level."""
        criteria = {"bloom_levels": ["invalid_level"]}
        with pytest.raises(ValidationError, match="Invalid bloom level"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_target_duration_valid(self):
        """Test target duration validation with valid input."""
        criteria = {"target_duration": 60}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["target_duration"] == 60

    def test_validate_target_duration_too_short(self):
        """Test target duration validation with duration too short."""
        criteria = {"target_duration": 4}
        with pytest.raises(ValidationError, match="Target duration must be between 5 and 300 minutes"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_target_duration_too_long(self):
        """Test target duration validation with duration too long."""
        criteria = {"target_duration": 301}
        with pytest.raises(ValidationError, match="Target duration must be between 5 and 300 minutes"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_topics_valid(self):
        """Test topics validation with valid input."""
        criteria = {"topics": ["algorithms", "patterns"]}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["topics"] == ["algorithms", "patterns"]

    def test_validate_topics_invalid(self):
        """Test topics validation with invalid topic."""
        criteria = {"topics": ["invalid_topic"]}
        with pytest.raises(ValidationError, match="Invalid topic"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_max_activity_count_valid(self):
        """Test max activity count validation with valid input."""
        criteria = {"max_activity_count": 5}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["max_activity_count"] == 5

    def test_validate_max_activity_count_too_low(self):
        """Test max activity count validation with count too low."""
        criteria = {"max_activity_count": 0}
        with pytest.raises(ValidationError, match="Max activity count must be between 1 and 10"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_max_activity_count_too_high(self):
        """Test max activity count validation with count too high."""
        criteria = {"max_activity_count": 11}
        with pytest.raises(ValidationError, match="Max activity count must be between 1 and 10"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_priority_categories_valid(self):
        """Test priority categories validation with valid input."""
        criteria = {"priority_categories": ["age_appropriateness", "topic_relevance"]}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["priority_categories"] == ["age_appropriateness", "topic_relevance"]

    def test_validate_priority_categories_invalid(self):
        """Test priority categories validation with invalid category."""
        criteria = {"priority_categories": ["invalid_category"]}
        with pytest.raises(ValidationError, match="Invalid priority category"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_limit_valid(self):
        """Test limit validation with valid input."""
        criteria = {"limit": 25}
        result = RecommendationFormValidator.validate_recommendation_criteria(criteria)
        assert result["limit"] == 25

    def test_validate_limit_too_low(self):
        """Test limit validation with limit too low."""
        criteria = {"limit": 0}
        with pytest.raises(ValidationError, match="Limit must be between 1 and 100"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_limit_too_high(self):
        """Test limit validation with limit too high."""
        criteria = {"limit": 101}
        with pytest.raises(ValidationError, match="Limit must be between 1 and 100"):
            RecommendationFormValidator.validate_recommendation_criteria(criteria)

    def test_validate_boolean_params_valid(self):
        """Test boolean parameters validation with valid input."""
        params = {
            "allow_lesson_plans": True,
            "include_breaks": False,
            "save_search": "true",
            "priority_age": "1",
            "priority_topics": "yes",
            "priority_duration": "on",
            "priority_bloom_level": "false",
        }
        result = RecommendationFormValidator.validate_boolean_params(params)
        assert result["allow_lesson_plans"] is True
        assert result["include_breaks"] is False
        assert result["save_search"] is True
        assert result["priority_age"] is True
        assert result["priority_topics"] is True
        assert result["priority_duration"] is True
        assert result["priority_bloom_level"] is False

    def test_validate_boolean_params_invalid(self):
        """Test boolean parameters validation with invalid input."""
        params = {"allow_lesson_plans": 123}  # Invalid type
        with pytest.raises(ValidationError, match="allow_lesson_plans must be a boolean value"):
            RecommendationFormValidator.validate_boolean_params(params)

    def test_validate_boolean_params_empty(self):
        """Test boolean parameters validation with empty input."""
        result = RecommendationFormValidator.validate_boolean_params({})
        assert result == {}


class TestActivityFormValidator:
    """Test ActivityFormValidator - comprehensive coverage."""

    def test_validate_activity_data_minimal_valid(self):
        """Test activity data validation with minimal valid input."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["name"] == "Test Activity"
        assert result["age_min"] == 8
        assert result["age_max"] == 12
        assert result["format"] == "digital"
        assert result["bloom_level"] == "understand"
        assert result["duration_min_minutes"] == 30

    def test_validate_activity_data_missing_required_field(self):
        """Test activity data validation with missing required field."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            # Missing bloom_level
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Required field 'bloom_level' is missing"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_name_empty(self):
        """Test activity data validation with empty name."""
        data = {
            "name": "",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Name cannot be empty"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_name_too_long(self):
        """Test activity data validation with name too long."""
        long_name = "a" * 256
        data = {
            "name": long_name,
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Name must be 255 characters or less"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_source_valid(self):
        """Test activity data validation with valid source."""
        data = {
            "name": "Test Activity",
            "source": "Test Source",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["source"] == "Test Source"

    def test_validate_activity_data_source_too_long(self):
        """Test activity data validation with source too long."""
        long_source = "a" * 256
        data = {
            "name": "Test Activity",
            "source": long_source,
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Source must be 255 characters or less"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_age_min_too_low(self):
        """Test activity data validation with age min too low."""
        data = {
            "name": "Test Activity",
            "age_min": 5,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Age min must be between 6 and 15"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_age_max_too_high(self):
        """Test activity data validation with age max too high."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 16,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Age max must be between 6 and 15"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_age_max_less_than_min(self):
        """Test activity data validation with age max less than min."""
        data = {
            "name": "Test Activity",
            "age_min": 12,
            "age_max": 8,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Age max must be greater than or equal to age min"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_format_invalid(self):
        """Test activity data validation with invalid format."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "invalid",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Invalid format"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_bloom_level_invalid(self):
        """Test activity data validation with invalid bloom level."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "invalid",
            "duration_min_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Invalid bloom level"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_duration_min_too_short(self):
        """Test activity data validation with duration min too short."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 0,
        }
        with pytest.raises(ValidationError, match="Duration min must be between 1 and 300 minutes"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_duration_max_too_long(self):
        """Test activity data validation with duration max too long."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "duration_max_minutes": 301,
        }
        with pytest.raises(ValidationError, match="Duration max must be between 1 and 300 minutes"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_duration_max_less_than_min(self):
        """Test activity data validation with duration max less than min."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 60,
            "duration_max_minutes": 30,
        }
        with pytest.raises(ValidationError, match="Duration max must be greater than or equal to duration min"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_mental_load_valid(self):
        """Test activity data validation with valid mental load."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "mental_load": "medium",
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["mental_load"] == "medium"

    def test_validate_activity_data_mental_load_invalid(self):
        """Test activity data validation with invalid mental load."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "mental_load": "invalid",
        }
        with pytest.raises(ValidationError, match="Invalid mental load"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_physical_energy_valid(self):
        """Test activity data validation with valid physical energy."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "physical_energy": "high",
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["physical_energy"] == "high"

    def test_validate_activity_data_prep_time_valid(self):
        """Test activity data validation with valid prep time."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "prep_time_minutes": 15,
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["prep_time_minutes"] == 15

    def test_validate_activity_data_prep_time_invalid_increment(self):
        """Test activity data validation with prep time not in 5-minute increments."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "prep_time_minutes": 13,
        }
        with pytest.raises(ValidationError, match="Prep time must be in 5-minute increments"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_cleanup_time_valid(self):
        """Test activity data validation with valid cleanup time."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "cleanup_time_minutes": 10,
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["cleanup_time_minutes"] == 10

    def test_validate_activity_data_resources_needed_valid(self):
        """Test activity data validation with valid resources needed."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "resources_needed": ["computers", "tablets"],
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["resources_needed"] == ["computers", "tablets"]

    def test_validate_activity_data_resources_needed_invalid_type(self):
        """Test activity data validation with invalid resources needed type."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "resources_needed": "not_a_list",
        }
        with pytest.raises(ValidationError, match="Resources needed must be an array"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_resources_needed_empty_strings(self):
        """Test activity data validation with empty strings in resources."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "resources_needed": ["computers", "", "tablets"],
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["resources_needed"] == ["computers", "tablets"]

    def test_validate_activity_data_resources_needed_too_long(self):
        """Test activity data validation with resource name too long."""
        long_resource = "a" * 51
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "resources_needed": [long_resource],
        }
        with pytest.raises(ValidationError, match="Resource name must be 50 characters or less"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_topics_valid(self):
        """Test activity data validation with valid topics."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "topics": ["algorithms", "patterns"],
        }
        result = ActivityFormValidator.validate_activity_data(data)
        assert result["topics"] == ["algorithms", "patterns"]

    def test_validate_activity_data_topics_invalid_type(self):
        """Test activity data validation with invalid topics type."""
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "topics": "not_a_list",
        }
        with pytest.raises(ValidationError, match="Topics must be an array"):
            ActivityFormValidator.validate_activity_data(data)

    def test_validate_activity_data_topics_too_long(self):
        """Test activity data validation with topic name too long."""
        long_topic = "a" * 51
        data = {
            "name": "Test Activity",
            "age_min": 8,
            "age_max": 12,
            "format": "digital",
            "bloom_level": "understand",
            "duration_min_minutes": 30,
            "topics": [long_topic],
        }
        with pytest.raises(ValidationError, match="Topic name must be 50 characters or less"):
            ActivityFormValidator.validate_activity_data(data)


class TestValidatePaginationParams:
    """Test validate_pagination_params function."""

    def test_validate_pagination_params_valid(self):
        """Test pagination params validation with valid input."""
        limit, offset = validate_pagination_params(25, 10)
        assert limit == 25
        assert offset == 10

    def test_validate_pagination_params_defaults(self):
        """Test pagination params validation with None values."""
        limit, offset = validate_pagination_params(None, None)
        assert limit == 50
        assert offset == 0

    def test_validate_pagination_params_limit_too_low(self):
        """Test pagination params validation with limit too low."""
        with pytest.raises(ValidationError, match="Limit must be at least 1"):
            validate_pagination_params(0, 0)

    def test_validate_pagination_params_limit_too_high(self):
        """Test pagination params validation with limit too high."""
        with pytest.raises(ValidationError, match="Limit cannot exceed 100"):
            validate_pagination_params(101, 0)

    def test_validate_pagination_params_offset_negative(self):
        """Test pagination params validation with negative offset."""
        with pytest.raises(ValidationError, match="Offset must be non-negative"):
            validate_pagination_params(25, -1)

    def test_validate_pagination_params_limit_invalid_type(self):
        """Test pagination params validation with invalid limit type."""
        with pytest.raises(ValidationError, match="Limit must be an integer"):
            validate_pagination_params("invalid", 0)

    def test_validate_pagination_params_offset_invalid_type(self):
        """Test pagination params validation with invalid offset type."""
        with pytest.raises(ValidationError, match="Offset must be an integer"):
            validate_pagination_params(25, "invalid")
