"""
Recommendation engine tests - Core business logic only.
Focuses on essential recommendation functionality.
"""

import pytest

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


class TestRecommendationEngine:
    """Test recommendation engine core business logic."""

    def _convert_criteria_to_typed(self, criteria: dict) -> tuple[SearchCriteria, list[PriorityCategory]]:
        """Helper to convert dict criteria to typed objects for testing."""
        # Extract priority categories
        priority_categories = []
        if "priority_categories" in criteria:
            priority_values = criteria["priority_categories"]
            if isinstance(priority_values, list):
                for category_value in priority_values:
                    try:
                        priority_categories.append(PriorityCategory(category_value))
                    except ValueError:
                        continue

        # Convert to SearchCriteria
        search_criteria = SearchCriteria(
            name=criteria.get("name"),
            target_age=criteria.get("target_age"),
            format=self._convert_format_list(criteria.get("format")),
            bloom_levels=self._convert_bloom_levels_list(criteria.get("bloom_levels")),
            target_duration=criteria.get("target_duration"),
            available_resources=self._convert_resources_list(criteria.get("available_resources")),
            preferred_topics=self._convert_topics_list(criteria.get("topics")),
        )

        return search_criteria, priority_categories

    def _convert_format_list(self, formats) -> list[ActivityFormat] | None:
        """Convert format list to ActivityFormat enums."""
        if not formats:
            return None
        if isinstance(formats, str):
            return [ActivityFormat(formats)]
        if isinstance(formats, list):
            return [ActivityFormat(f) for f in formats if f]
        return None

    def _convert_bloom_levels_list(self, bloom_levels) -> list[BloomLevel] | None:
        """Convert bloom levels list to BloomLevel enums."""
        if not bloom_levels:
            return None
        if isinstance(bloom_levels, str):
            return [BloomLevel(bloom_levels)]
        if isinstance(bloom_levels, list):
            return [BloomLevel(b) for b in bloom_levels if b]
        return None

    def _convert_resources_list(self, resources) -> list[ActivityResource] | None:
        """Convert resources list to ActivityResource enums."""
        if not resources:
            return None
        if isinstance(resources, str):
            return [ActivityResource(resources)]
        if isinstance(resources, list):
            return [ActivityResource(r) for r in resources if r]
        return None

    def _convert_topics_list(self, topics) -> list[ActivityTopic] | None:
        """Convert topics list to ActivityTopic enums."""
        if not topics:
            return None
        if isinstance(topics, str):
            return [ActivityTopic(topics)]
        if isinstance(topics, list):
            return [ActivityTopic(t) for t in topics if t]
        return None

    @pytest.fixture
    def sample_activities(self):
        """Create sample activities for testing."""
        return [
            ActivityModel(
                id=1,
                name="Binary Search Activity",
                description="Learn binary search algorithm through hands-on activities",
                source="CS Unplugged",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.UNDERSTAND,
                duration_min_minutes=30,
                duration_max_minutes=45,
                mental_load=EnergyLevel.LOW,
                physical_energy=EnergyLevel.LOW,
                prep_time_minutes=5,
                cleanup_time_minutes=5,
                topics=[ActivityTopic.ALGORITHMS],
                resources_needed=[ActivityResource.HANDOUTS],
            ),
            ActivityModel(
                id=2,
                name="Python Turtle Graphics",
                description="Create digital art using Python turtle graphics programming",
                source="Python.org",
                age_min=12,
                age_max=16,
                format=ActivityFormat.DIGITAL,
                bloom_level=BloomLevel.CREATE,
                duration_min_minutes=45,
                duration_max_minutes=60,
                mental_load=EnergyLevel.HIGH,
                physical_energy=EnergyLevel.LOW,
                prep_time_minutes=10,
                cleanup_time_minutes=5,
                topics=[ActivityTopic.ABSTRACTION, ActivityTopic.PATTERNS],
                resources_needed=[ActivityResource.COMPUTERS],
            ),
        ]

    def test_basic_recommendation_scoring(self, sample_activities):
        """Test basic recommendation scoring logic with category breakdown."""
        criteria = {"target_age": 12, "format": "unplugged", "topics": ["algorithms"]}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)

        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        # Should return recommendations
        assert len(recommendations) > 0

        # Each recommendation should be a tuple of (activities_list, score_obj)
        for activities_list, score_obj in recommendations:
            assert len(activities_list) > 0
            assert hasattr(score_obj, "total_score")
            assert 0 <= score_obj.total_score <= 100

            # Test new category-based scoring structure
            assert hasattr(score_obj, "category_scores")
            assert isinstance(score_obj.category_scores, dict)
            assert len(score_obj.category_scores) > 0

            # Test each category score
            for _category_name, category_score in score_obj.category_scores.items():
                assert hasattr(category_score, "score")
                assert hasattr(category_score, "impact")
                assert hasattr(category_score, "priority_multiplier")
                assert 0 <= category_score.score <= 100
                assert 1 <= category_score.impact <= 5
                assert category_score.priority_multiplier >= 1.0

    def test_recommendation_with_priority_categories(self, sample_activities):
        """Test recommendation scoring with priority categories."""
        criteria = {
            "target_age": 12,
            "format": "unplugged",
            "topics": ["algorithms"],
            "priority_categories": ["age_appropriateness", "topic_relevance"],
        }
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)

        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        assert len(recommendations) > 0

        # Check that priority categories are applied
        for _activities_list, score_obj in recommendations:
            assert score_obj.priority_categories == priority_categories

            # Check that priority categories have multipliers
            for category_name, category_score in score_obj.category_scores.items():
                if category_name in ["age_appropriateness", "topic_relevance"]:
                    assert category_score.priority_multiplier > 1.0

    def test_format_filtering(self, sample_activities):
        """Test format filtering logic."""
        criteria = {"format": "digital"}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)
        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        # Should only return digital activities
        assert len(recommendations) > 0
        activities_list, score = recommendations[0]
        for activity in activities_list:
            if hasattr(activity, "format"):
                assert activity.format == ActivityFormat.DIGITAL

    def test_topic_matching(self, sample_activities):
        """Test topic matching logic."""
        criteria = {"topics": ["algorithms"]}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)
        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        # Should return activities with matching topics
        assert len(recommendations) > 0
        activities_list, score = recommendations[0]
        assert len(activities_list) > 0

    def test_bloom_level_filtering(self, sample_activities):
        """Test bloom level filtering logic."""
        criteria = {"bloom_levels": ["create"]}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)
        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        # Should only return activities with create bloom level
        assert len(recommendations) > 0
        activities_list, score = recommendations[0]
        for activity in activities_list:
            if hasattr(activity, "bloom_level"):
                assert activity.bloom_level == BloomLevel.CREATE

    def test_duration_constraints(self, sample_activities):
        """Test duration constraint handling."""
        criteria = {"target_duration": 30}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)
        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        # Should return activities that fit duration constraint
        assert len(recommendations) > 0
        activities_list, score = recommendations[0]
        assert len(activities_list) > 0

    def test_empty_criteria_handling(self, sample_activities):
        """Test handling of empty criteria."""
        criteria = {}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)
        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=False,
            max_activity_count=1,
            limit=1,
        )

        # Should return some activities even with empty criteria
        assert len(recommendations) > 0
        activities_list, score = recommendations[0]
        assert len(activities_list) > 0

    def test_break_generation(self, sample_activities):
        """Test break generation logic."""
        criteria = {"target_duration": 60}
        search_criteria, priority_categories = self._convert_criteria_to_typed(criteria)
        recommendations = get_recommendations(
            criteria=search_criteria,
            activities=sample_activities,
            priority_categories=priority_categories,
            include_breaks=True,
            max_activity_count=2,
            limit=1,
        )

        # Should include breaks when requested
        assert len(recommendations) > 0
        activities_list, score = recommendations[0]
        assert len(activities_list) > 0

        # Should have both activities and potentially breaks
        activities = [a for a in activities_list if hasattr(a, "name")]
        assert len(activities) > 0
