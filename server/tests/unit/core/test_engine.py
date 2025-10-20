"""
RecommendationPipeline tests - High-value business logic only.
Focuses on core recommendation engine functionality and critical edge cases.
"""

import pytest

from app.core.constants import DEFAULT_MAX_ACTIVITY_COUNT, DEFAULT_RECOMMENDATION_LIMIT
from app.core.engine import RecommendationPipeline, get_recommendations
from app.core.models import (
    ActivityFormat,
    ActivityModel,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    PriorityCategory,
    SearchCriteria,
)


class TestRecommendationPipeline:
    """Test RecommendationPipeline core functionality."""

    @pytest.fixture
    def sample_activities(self):
        """Create sample activities for testing."""
        return [
            ActivityModel(
                id=1,
                name="Activity 1",
                description="First test activity",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.UNDERSTAND,
                duration_min_minutes=30,
                topics=[ActivityTopic.ALGORITHMS],
                resources_needed=[ActivityResource.HANDOUTS],
            ),
            ActivityModel(
                id=2,
                name="Activity 2",
                description="Second test activity",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.APPLY,
                duration_min_minutes=45,
                topics=[ActivityTopic.PATTERNS],
                resources_needed=[ActivityResource.HANDOUTS],
            ),
        ]

    @pytest.fixture
    def sample_criteria(self):
        """Create sample search criteria."""
        return SearchCriteria(
            target_age=12,
            format=[ActivityFormat.UNPLUGGED],
            bloom_levels=[BloomLevel.UNDERSTAND, BloomLevel.APPLY],
            target_duration=30,
            available_resources=[ActivityResource.HANDOUTS],
            preferred_topics=[ActivityTopic.ALGORITHMS, ActivityTopic.PATTERNS],
        )

    def test_pipeline_initialization(self, sample_criteria):
        """Test RecommendationPipeline initialization with various parameters."""
        priority_categories = [PriorityCategory.AGE_APPROPRIATENESS]

        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=priority_categories,
            include_breaks=True,
            max_activity_count=3,
            limit=5,
        )

        assert pipeline.criteria == sample_criteria
        assert pipeline.priority_categories == priority_categories
        assert pipeline.include_breaks is True
        assert pipeline.max_activity_count == 3
        assert pipeline.limit == 5
        assert pipeline.scoring_engine is not None

    def test_pipeline_default_parameters(self, sample_criteria):
        """Test RecommendationPipeline with default parameters."""
        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=[],
        )

        assert pipeline.include_breaks is False
        assert pipeline.max_activity_count == DEFAULT_MAX_ACTIVITY_COUNT
        assert pipeline.limit == DEFAULT_RECOMMENDATION_LIMIT

    def test_process_single_activity(self, sample_activities, sample_criteria):
        """Test pipeline processing with single activity."""
        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=[],
            max_activity_count=1,
            limit=1,
        )

        results = pipeline.process(sample_activities)

        assert len(results) == 1
        activities, score = results[0]
        assert len(activities) == 1
        assert isinstance(score.total_score, (int, float))
        assert score.is_sequence is False
        assert score.activity_count == 1

    def test_process_multiple_activities_with_breaks(self, sample_activities, sample_criteria):
        """Test pipeline processing with multiple activities and breaks."""
        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=[],
            include_breaks=True,
            max_activity_count=2,
            limit=1,
        )

        results = pipeline.process(sample_activities)

        assert len(results) == 1
        activities, score = results[0]
        assert len(activities) >= 1  # Should have at least the activities
        # Note: is_sequence depends on implementation details
        assert score.activity_count >= 1

    def test_process_with_priority_categories(self, sample_activities, sample_criteria):
        """Test pipeline processing with priority categories."""
        priority_categories = [PriorityCategory.AGE_APPROPRIATENESS, PriorityCategory.TOPIC_RELEVANCE]
        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=priority_categories,
            max_activity_count=1,
            limit=1,
        )

        results = pipeline.process(sample_activities)

        assert len(results) == 1
        activities, score = results[0]
        assert score.priority_categories == priority_categories

    def test_process_empty_activities(self, sample_criteria):
        """Test pipeline processing with empty activities list."""
        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=[],
        )

        results = pipeline.process([])
        assert results == []

    def test_process_with_limit(self, sample_activities, sample_criteria):
        """Test pipeline processing respects limit parameter."""
        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=[],
            max_activity_count=1,
            limit=1,
        )

        # Create more activities than limit
        many_activities = sample_activities * 5
        results = pipeline.process(many_activities)

        assert len(results) <= 1  # Should respect limit

    def test_get_recommendations_function(self, sample_activities, sample_criteria):
        """Test the get_recommendations function interface."""
        priority_categories = [PriorityCategory.AGE_APPROPRIATENESS]

        results = get_recommendations(
            criteria=sample_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=2,
        )

        assert isinstance(results, list)
        assert len(results) <= 2  # Should respect limit

        for activities, score in results:
            assert isinstance(activities, list)
            assert len(activities) <= 1  # Should respect max_activity_count
            assert score.priority_categories == priority_categories

    def test_edge_case_invalid_criteria(self, sample_activities):
        """Test pipeline with edge case criteria."""
        # Test with minimal criteria
        minimal_criteria = SearchCriteria(target_age=12)

        pipeline = RecommendationPipeline(
            criteria=minimal_criteria,
            priority_categories=[],
        )

        results = pipeline.process(sample_activities)
        assert isinstance(results, list)  # Should handle gracefully

    def test_edge_case_single_activity_sequence(self, sample_criteria):
        """Test sequence scoring with single activity."""
        single_activity = [
            ActivityModel(
                id=1,
                name="Single Activity",
                description="Test single activity sequence",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.UNDERSTAND,
                duration_min_minutes=30,
                topics=[ActivityTopic.ALGORITHMS],
                resources_needed=[ActivityResource.HANDOUTS],
            )
        ]

        pipeline = RecommendationPipeline(
            criteria=sample_criteria,
            priority_categories=[],
            max_activity_count=1,
            limit=1,
        )

        results = pipeline.process(single_activity)
        assert len(results) == 1
        activities, score = results[0]
        assert len(activities) == 1
        assert score.activity_count == 1
