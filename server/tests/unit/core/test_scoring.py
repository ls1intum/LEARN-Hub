"""
ScoringEngine tests.
Focuses on core scoring algorithms and critical edge cases.
"""

import pytest

from app.core.constants import SCORING_CATEGORIES
from app.core.models import (
    ActivityFormat,
    ActivityModel,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    CategoryScore,
    EnergyLevel,
    PriorityCategory,
    ScoreModel,
    SearchCriteria,
)
from app.core.scoring import ScoringEngine


class TestScoringEngine:
    """Test ScoringEngine core functionality."""

    @pytest.fixture
    def sample_activity(self):
        """Create a sample activity for testing."""
        return ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
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
            topics=[ActivityTopic.ALGORITHMS],
            resources_needed=[ActivityResource.HANDOUTS],
        )

    @pytest.fixture
    def sample_criteria(self):
        """Create sample search criteria."""
        return SearchCriteria(
            target_age=12,
            format=[ActivityFormat.UNPLUGGED],
            bloom_levels=[BloomLevel.UNDERSTAND],
            target_duration=30,
            available_resources=[ActivityResource.HANDOUTS],
            preferred_topics=[ActivityTopic.ALGORITHMS],
        )

    def test_scoring_engine_initialization(self):
        """Test ScoringEngine initialization with and without priority categories."""
        # Test without priority categories
        engine = ScoringEngine()
        assert engine.priority_categories == []

        # Test with priority categories
        priority_categories = [PriorityCategory.AGE_APPROPRIATENESS, PriorityCategory.TOPIC_RELEVANCE]
        engine = ScoringEngine(priority_categories)
        assert engine.priority_categories == priority_categories

    def test_score_activity_basic(self, sample_activity, sample_criteria):
        """Test basic activity scoring with category breakdown."""
        engine = ScoringEngine()
        score_model = engine.score_activity(sample_activity, sample_criteria)

        assert isinstance(score_model, ScoreModel)
        assert 0 <= score_model.total_score <= 100
        assert score_model.is_sequence is False
        assert score_model.activity_count == 1
        assert len(score_model.category_scores) == 4  # age, bloom, topic, duration
        assert score_model.priority_categories == []

        # Test category scores structure
        for category_score in score_model.category_scores.values():
            assert isinstance(category_score, CategoryScore)
            assert 0 <= category_score.score <= 100
            assert 1 <= category_score.impact <= 5
            assert category_score.priority_multiplier >= 1.0

    def test_score_activity_with_priority_categories(self, sample_activity, sample_criteria):
        """Test activity scoring with priority categories."""
        priority_categories = [PriorityCategory.AGE_APPROPRIATENESS, PriorityCategory.TOPIC_RELEVANCE]
        engine = ScoringEngine(priority_categories)
        score_model = engine.score_activity(sample_activity, sample_criteria)

        assert score_model.priority_categories == priority_categories

        # Check that priority categories have multipliers applied
        for category_name, category_score in score_model.category_scores.items():
            if category_name in ["age_appropriateness", "topic_relevance"]:
                assert category_score.priority_multiplier > 1.0
            else:
                assert category_score.priority_multiplier == 1.0

    def test_score_sequence_with_series_cohesion(self, sample_criteria):
        """Test sequence scoring includes series cohesion category."""
        activities = [
            ActivityModel(
                id=1,
                name="Activity 1",
                description="First test activity for scoring",
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
                description="Second test activity for scoring",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.APPLY,
                duration_min_minutes=30,
                topics=[ActivityTopic.ALGORITHMS],
                resources_needed=[ActivityResource.HANDOUTS],
            ),
        ]

        engine = ScoringEngine()
        score_model = engine.score_sequence(activities, sample_criteria)

        assert score_model.is_sequence is True
        assert score_model.activity_count == 2
        assert "series_cohesion" in score_model.category_scores

    def test_age_appropriateness_scoring_perfect_match(self, sample_criteria):
        """Test age appropriateness scoring with perfect match."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for age scoring",
            age_min=12,
            age_max=12,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
        )

        engine = ScoringEngine()
        score_model = engine.score_activity(activity, sample_criteria)
        age_score = score_model.category_scores["age_appropriateness"]

        assert age_score.score == 100
        assert age_score.category == "age_appropriateness"
        assert age_score.impact == SCORING_CATEGORIES["age_appropriateness"].impact

    def test_age_appropriateness_scoring_no_match(self, sample_criteria):
        """Test age appropriateness scoring with no match."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
            age_min=20,  # Far from target age 12
            age_max=25,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
        )

        engine = ScoringEngine()
        score_model = engine.score_activity(activity, sample_criteria)
        age_score = score_model.category_scores["age_appropriateness"]

        assert age_score.score == 0

    def test_bloom_level_match_perfect(self, sample_criteria):
        """Test Bloom level matching with perfect match."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,  # Matches criteria
            duration_min_minutes=30,
        )

        engine = ScoringEngine()
        score_model = engine.score_activity(activity, sample_criteria)
        bloom_score = score_model.category_scores["bloom_level_match"]

        assert bloom_score.score == 100

    def test_topic_relevance_perfect_match(self, sample_criteria):
        """Test topic relevance with perfect match."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
            topics=[ActivityTopic.ALGORITHMS],  # Matches criteria
        )

        engine = ScoringEngine()
        score_model = engine.score_activity(activity, sample_criteria)
        topic_score = score_model.category_scores["topic_relevance"]

        assert topic_score.score == 100

    def test_topic_relevance_partial_match(self, sample_criteria):
        """Test topic relevance with partial match."""
        # Update criteria to have multiple topics
        criteria = SearchCriteria(
            target_age=12,
            preferred_topics=[ActivityTopic.ALGORITHMS, ActivityTopic.PATTERNS],
        )

        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
            topics=[ActivityTopic.ALGORITHMS],  # Only 1 of 2 topics
        )

        engine = ScoringEngine()
        score_model = engine.score_activity(activity, criteria)
        topic_score = score_model.category_scores["topic_relevance"]

        assert topic_score.score == 50  # 50% match

    def test_duration_fit_perfect_match(self, sample_activity):
        """Test duration fit with perfect match."""
        # Create activity with exact duration match
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
            age_min=10,
            age_max=14,
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,
            duration_min_minutes=30,
            duration_max_minutes=30,  # Same as min for exact match
        )

        criteria = SearchCriteria(target_duration=30)  # Matches activity duration
        engine = ScoringEngine()
        score_model = engine.score_activity(activity, criteria)
        duration_score = score_model.category_scores["duration_fit"]

        assert duration_score.score == 100

    def test_duration_fit_over_target(self, sample_activity):
        """Test duration fit when over target."""
        criteria = SearchCriteria(target_duration=20)  # Lower than activity duration
        engine = ScoringEngine()
        score_model = engine.score_activity(sample_activity, criteria)
        duration_score = score_model.category_scores["duration_fit"]

        assert duration_score.score < 100  # Over target gets partial score

    def test_priority_category_multiplier(self, sample_activity, sample_criteria):
        """Test priority category multiplier application."""
        priority_categories = [PriorityCategory.AGE_APPROPRIATENESS]
        engine = ScoringEngine(priority_categories)
        score_model = engine.score_activity(sample_activity, sample_criteria)

        age_score = score_model.category_scores["age_appropriateness"]
        assert age_score.priority_multiplier == 2.0  # PRIORITY_CATEGORY_MULTIPLIER

    def test_series_cohesion_multiple_activities(self):
        """Test series cohesion for multiple activities."""
        activities = [
            ActivityModel(
                id=1,
                name="Activity 1",
                description="First activity for series cohesion testing",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.REMEMBER,
                duration_min_minutes=15,
                topics=[ActivityTopic.ALGORITHMS],
            ),
            ActivityModel(
                id=2,
                name="Activity 2",
                description="Second activity for series cohesion testing",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.UNDERSTAND,  # Progressive Bloom level
                duration_min_minutes=20,
                topics=[ActivityTopic.ALGORITHMS],  # Same topic
            ),
        ]

        engine = ScoringEngine()
        cohesion_score = engine._score_series_cohesion_category(activities)

        assert 0 <= cohesion_score.score <= 100
        assert cohesion_score.category == "series_cohesion"

    def test_edge_case_missing_activity_data(self, sample_criteria):
        """Test scoring with missing activity data."""
        activity = ActivityModel(
            id=1,
            name="Test Activity",
            description="Test activity for scoring validation",
            age_min=10,  # Required field
            age_max=14,  # Required field
            format=ActivityFormat.UNPLUGGED,
            bloom_level=BloomLevel.UNDERSTAND,  # Required field
            duration_min_minutes=30,
            # Missing topics (optional field)
        )

        engine = ScoringEngine()
        score_model = engine.score_activity(activity, sample_criteria)

        # Should handle missing data gracefully
        assert isinstance(score_model, ScoreModel)
        assert 0 <= score_model.total_score <= 100
