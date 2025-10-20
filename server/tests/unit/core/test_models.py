"""
Core models tests - Essential model functionality only.
Focuses on critical model creation, validation, and core business logic.
"""

import pytest
from pydantic import ValidationError

from app.core.models import (
    ActivityFormat,
    ActivityModel,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    Break,
    CategoryScore,
    EnergyLevel,
    PriorityCategory,
    ScoreModel,
    SearchCriteria,
)


class TestEnums:
    """Test enum classes for proper values and behavior."""

    def test_activity_format_enum(self):
        """Test ActivityFormat enum values."""
        assert ActivityFormat.UNPLUGGED == "unplugged"
        assert ActivityFormat.DIGITAL == "digital"
        assert ActivityFormat.HYBRID == "hybrid"

        # Test enum iteration
        formats = list(ActivityFormat)
        assert len(formats) == 3
        assert ActivityFormat.UNPLUGGED in formats

    def test_priority_category_enum(self):
        """Test PriorityCategory enum values."""
        assert PriorityCategory.AGE_APPROPRIATENESS == "age_appropriateness"
        assert PriorityCategory.TOPIC_RELEVANCE == "topic_relevance"
        assert PriorityCategory.BLOOM_LEVEL_MATCH == "bloom_level_match"
        assert PriorityCategory.DURATION_FIT == "duration_fit"

    def test_bloom_level_enum(self):
        """Test BloomLevel enum values."""
        assert BloomLevel.REMEMBER == "remember"
        assert BloomLevel.UNDERSTAND == "understand"
        assert BloomLevel.APPLY == "apply"
        assert BloomLevel.ANALYZE == "analyze"
        assert BloomLevel.EVALUATE == "evaluate"
        assert BloomLevel.CREATE == "create"


class TestActivityModel:
    """Test ActivityModel creation and validation."""

    def test_activity_model_creation_basic(self):
        """Test basic ActivityModel creation."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity description",
            source="Test Source",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
        )

        assert activity.id == 1
        assert activity.name == "Test Activity"
        assert activity.age_min == 10
        assert activity.age_max == 14
        assert activity.format == ActivityFormat.UNPLUGGED
        assert activity.bloom_level == BloomLevel.UNDERSTAND
        assert activity.duration_min_minutes == 30

    def test_activity_model_with_optional_fields(self):
        """Test ActivityModel with optional fields."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity description",
            source="Test Source",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
            duration_max_minutes=45,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=5,
            topics=[ActivityTopic.ALGORITHMS, ActivityTopic.PATTERNS],
            resources_needed=[ActivityResource.HANDOUTS, ActivityResource.COMPUTERS],
        )

        assert activity.duration_max_minutes == 45
        assert activity.mental_load == EnergyLevel.MEDIUM
        assert activity.physical_energy == EnergyLevel.LOW
        assert activity.prep_time_minutes == 5
        assert activity.cleanup_time_minutes == 5
        assert ActivityTopic.ALGORITHMS in activity.topics
        assert ActivityResource.HANDOUTS in activity.resources_needed

    def test_activity_model_validation_errors(self):
        """Test ActivityModel validation with invalid data."""
        # Test missing required fields
        with pytest.raises(ValidationError):
            ActivityModel(
                id=1,
                name="Test Activity",
                # Missing required fields
            )

        # Test invalid enum values
        with pytest.raises(ValidationError):
            ActivityModel(
                id=1,
                name="Test Activity",
                description="Test activity description",
                source="Test Source",
                age_min=10,
                age_max=14,
                format="invalid_format",  # Invalid enum
                bloom_level=BloomLevel.UNDERSTAND,
                duration_min_minutes=30,
            )

    def test_activity_model_edge_cases(self):
        """Test ActivityModel edge cases."""
        # Test with empty lists
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity description",
            source="Test Source",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
            topics=[],  # Empty list
            resources_needed=[],  # Empty list
        )

        assert activity.topics == []
        assert activity.resources_needed == []


class TestSearchCriteria:
    """Test SearchCriteria creation and validation."""

    def test_search_criteria_creation_basic(self):
        """Test basic SearchCriteria creation."""
        criteria = SearchCriteria(
            target_age=12,
            format=[ActivityFormat.UNPLUGGED],
            bloom_levels=[BloomLevel.UNDERSTAND],
            target_duration=30,
        )

        assert criteria.target_age == 12
        assert criteria.format == [ActivityFormat.UNPLUGGED]
        assert criteria.bloom_levels == [BloomLevel.UNDERSTAND]
        assert criteria.target_duration == 30

    def test_search_criteria_with_all_fields(self):
        """Test SearchCriteria with all fields."""
        criteria = SearchCriteria(
            name="Test Search",
            target_age=12,
            format=[ActivityFormat.UNPLUGGED, ActivityFormat.DIGITAL],
            bloom_levels=[BloomLevel.UNDERSTAND, BloomLevel.APPLY],
            target_duration=60,
            available_resources=[ActivityResource.HANDOUTS, ActivityResource.COMPUTERS],
            preferred_topics=[ActivityTopic.ALGORITHMS, ActivityTopic.PATTERNS],
        )

        assert criteria.name == "Test Search"
        assert len(criteria.format) == 2
        assert len(criteria.bloom_levels) == 2
        assert criteria.target_duration == 60
        assert len(criteria.available_resources) == 2
        assert len(criteria.preferred_topics) == 2

    def test_search_criteria_validation(self):
        """Test SearchCriteria validation."""
        # Since SearchCriteria is a dataclass, it doesn't have validation
        # Test that it accepts valid data
        criteria = SearchCriteria(
            target_age=12,
            format=[ActivityFormat.UNPLUGGED],  # Valid enum
            bloom_levels=[BloomLevel.UNDERSTAND],
        )
        assert criteria.target_age == 12
        assert criteria.format == [ActivityFormat.UNPLUGGED]
        assert criteria.bloom_levels == [BloomLevel.UNDERSTAND]


class TestScoreModel:
    """Test ScoreModel creation and functionality."""

    def test_score_model_creation(self):
        """Test ScoreModel creation."""
        category_scores = {
            "age_appropriateness": CategoryScore(
                category="age_appropriateness", score=85, impact=3, priority_multiplier=1.0
            ),
            "topic_relevance": CategoryScore(category="topic_relevance", score=90, impact=4, priority_multiplier=2.0),
        }

        score_model = ScoreModel(
            total_score=87,
            is_sequence=False,
            activity_count=1,
            category_scores=category_scores,
            priority_categories=[PriorityCategory.AGE_APPROPRIATENESS],
        )

        assert score_model.total_score == 87
        assert score_model.is_sequence is False
        assert score_model.activity_count == 1
        assert len(score_model.category_scores) == 2
        assert PriorityCategory.AGE_APPROPRIATENESS in score_model.priority_categories

    def test_score_model_sequence(self):
        """Test ScoreModel for sequences."""
        category_scores = {
            "age_appropriateness": CategoryScore(
                category="age_appropriateness", score=80, impact=3, priority_multiplier=1.0
            ),
            "series_cohesion": CategoryScore(category="series_cohesion", score=75, impact=3, priority_multiplier=1.0),
        }

        score_model = ScoreModel(
            total_score=78,
            is_sequence=True,
            activity_count=3,
            category_scores=category_scores,
            priority_categories=[],
        )

        assert score_model.total_score == 78
        assert score_model.is_sequence is True
        assert score_model.activity_count == 3
        assert "series_cohesion" in score_model.category_scores


class TestBreak:
    """Test Break model creation and functionality."""

    def test_break_creation(self):
        """Test Break creation."""
        break_obj = Break(
            duration=5,
            description="Mental rest break",
            reasons=["High mental load", "Format transition"],
        )

        assert break_obj.duration == 5
        assert break_obj.description == "Mental rest break"
        assert len(break_obj.reasons) == 2
        assert "High mental load" in break_obj.reasons

    def test_break_validation(self):
        """Test Break model creation."""
        # Since Break is a Pydantic model, test valid creation
        break_obj = Break(
            duration=5,
            description="Test break",
            reasons=["Test reason"],
        )
        assert break_obj.duration == 5
        assert break_obj.description == "Test break"
        assert break_obj.reasons == ["Test reason"]
        assert break_obj.type == "break"


class TestCategoryScore:
    """Test CategoryScore creation and functionality."""

    def test_category_score_creation(self):
        """Test CategoryScore creation."""
        category_score = CategoryScore(
            category="age_appropriateness",
            score=85,
            impact=3,
            priority_multiplier=2.0,
        )

        assert category_score.category == "age_appropriateness"
        assert category_score.score == 85
        assert category_score.impact == 3
        assert category_score.priority_multiplier == 2.0

    def test_category_score_validation(self):
        """Test CategoryScore model creation."""
        # Since CategoryScore is a dataclass, test valid creation
        score = CategoryScore(
            category="test",
            score=85,  # Valid score
            impact=3,
            priority_multiplier=1.0,
        )
        assert score.category == "test"
        assert score.score == 85
        assert score.impact == 3
        assert score.priority_multiplier == 1.0
        assert not score.is_priority
