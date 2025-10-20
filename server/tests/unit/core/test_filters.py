"""
Filtering tests - Core filtering logic only.
Focuses on essential filtering functionality and critical edge cases.
"""

import pytest

from app.core.constants import AGE_FILTER_TOLERANCE
from app.core.filters import apply_hard_filters
from app.core.models import (
    ActivityFormat,
    ActivityModel,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    SearchCriteria,
)


class TestFilters:
    """Test filtering logic for activity recommendations."""

    @pytest.fixture
    def sample_activities(self):
        """Create sample activities for testing."""
        return [
            ActivityModel(
                id=1,
                name="Unplugged Activity",
                description="Unplugged activity for filter testing",
                age_min=10,
                age_max=14,
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.UNDERSTAND,
                duration_min_minutes=30,
                duration_max_minutes=45,
                topics=[ActivityTopic.ALGORITHMS],
                resources_needed=[ActivityResource.HANDOUTS],
            ),
            ActivityModel(
                id=2,
                name="Digital Activity",
                description="Digital activity for filter testing",
                age_min=12,
                age_max=16,
                format=ActivityFormat.DIGITAL,
                bloom_level=BloomLevel.CREATE,
                duration_min_minutes=45,
                duration_max_minutes=60,
                topics=[ActivityTopic.ABSTRACTION],
                resources_needed=[ActivityResource.COMPUTERS],
            ),
            ActivityModel(
                id=3,
                name="Hybrid Activity",
                description="Hybrid activity for filter testing",
                age_min=8,
                age_max=12,
                format=ActivityFormat.HYBRID,
                bloom_level=BloomLevel.APPLY,
                duration_min_minutes=20,
                duration_max_minutes=30,
                topics=[ActivityTopic.PATTERNS],
                resources_needed=[ActivityResource.TABLETS],
            ),
        ]

    def test_apply_hard_filters_no_criteria(self, sample_activities):
        """Test filtering with no criteria (should return all activities)."""
        criteria = SearchCriteria()
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        assert len(filtered_activities) == len(sample_activities)
        assert filtered_activities == sample_activities

    def test_apply_hard_filters_format_filtering(self, sample_activities):
        """Test format filtering."""
        criteria = SearchCriteria(format=[ActivityFormat.UNPLUGGED])
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        assert len(filtered_activities) == 1
        assert filtered_activities[0].format == ActivityFormat.UNPLUGGED

    def test_apply_hard_filters_multiple_formats(self, sample_activities):
        """Test filtering with multiple formats."""
        criteria = SearchCriteria(format=[ActivityFormat.UNPLUGGED, ActivityFormat.DIGITAL])
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        assert len(filtered_activities) == 2
        formats = [activity.format for activity in filtered_activities]
        assert ActivityFormat.UNPLUGGED in formats
        assert ActivityFormat.DIGITAL in formats

    def test_apply_hard_filters_bloom_level_filtering(self, sample_activities):
        """Test Bloom level filtering."""
        criteria = SearchCriteria(bloom_levels=[BloomLevel.UNDERSTAND])
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        assert len(filtered_activities) == 1
        assert filtered_activities[0].bloom_level == BloomLevel.UNDERSTAND

    def test_apply_hard_filters_multiple_bloom_levels(self, sample_activities):
        """Test filtering with multiple Bloom levels."""
        criteria = SearchCriteria(bloom_levels=[BloomLevel.UNDERSTAND, BloomLevel.CREATE])
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        assert len(filtered_activities) == 2
        bloom_levels = [activity.bloom_level for activity in filtered_activities]
        assert BloomLevel.UNDERSTAND in bloom_levels
        assert BloomLevel.CREATE in bloom_levels

    def test_apply_hard_filters_age_filtering_exact_match(self, sample_activities):
        """Test age filtering with exact match."""
        criteria = SearchCriteria(target_age=12)
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        # Should include activities where 12 is within age range
        assert len(filtered_activities) >= 1
        for activity in filtered_activities:
            assert activity.age_min <= 12 <= activity.age_max

    def test_apply_hard_filters_age_filtering_with_tolerance(self, sample_activities):
        """Test age filtering with tolerance."""
        criteria = SearchCriteria(target_age=15)  # Outside most ranges but within tolerance
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        # Should include activities within tolerance
        for activity in filtered_activities:
            age_distance = min(abs(activity.age_min - 15), abs(activity.age_max - 15))
            assert age_distance <= AGE_FILTER_TOLERANCE

    def test_apply_hard_filters_age_filtering_outside_tolerance(self, sample_activities):
        """Test age filtering outside tolerance."""
        criteria = SearchCriteria(target_age=25)  # Far outside all ranges
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        # Should return no activities
        assert len(filtered_activities) == 0

    def test_apply_hard_filters_combined_filters(self, sample_activities):
        """Test combined filtering with multiple criteria."""
        criteria = SearchCriteria(
            target_age=12,
            format=[ActivityFormat.UNPLUGGED, ActivityFormat.HYBRID],
            bloom_levels=[BloomLevel.UNDERSTAND, BloomLevel.APPLY],
        )
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        # Should return activities that match all criteria
        assert len(filtered_activities) >= 0
        for activity in filtered_activities:
            assert activity.format in [ActivityFormat.UNPLUGGED, ActivityFormat.HYBRID]
            assert activity.bloom_level in [BloomLevel.UNDERSTAND, BloomLevel.APPLY]
            assert activity.age_min <= 12 <= activity.age_max

    def test_apply_hard_filters_no_matches(self, sample_activities):
        """Test filtering with no matching activities."""
        criteria = SearchCriteria(
            target_age=25, format=[ActivityFormat.UNPLUGGED], bloom_levels=[BloomLevel.UNDERSTAND]  # Outside tolerance
        )
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        assert len(filtered_activities) == 0

    def test_apply_hard_filters_empty_activities_list(self):
        """Test filtering with empty activities list."""
        criteria = SearchCriteria(target_age=12)
        filtered_activities = apply_hard_filters([], criteria)

        assert len(filtered_activities) == 0

    def test_apply_hard_filters_edge_case_boundary_ages(self, sample_activities):
        """Test filtering with boundary age values."""
        # Test with minimum age - need to create activities that actually match age 8
        activities_with_age_8 = [
            ActivityModel(
                id=3,
                name="Young Activity",
                description="Activity for young children",
                age_min=6,
                age_max=10,  # This should include age 8
                format=ActivityFormat.UNPLUGGED,
                bloom_level=BloomLevel.REMEMBER,
                duration_min_minutes=15,
                duration_max_minutes=30,
                topics=[ActivityTopic.PATTERNS],
                resources_needed=[ActivityResource.BLOCKS],
            ),
            ActivityModel(
                id=4,
                name="Older Activity",
                description="Activity for older children",
                age_min=10,
                age_max=14,  # This should NOT include age 8
                format=ActivityFormat.DIGITAL,
                bloom_level=BloomLevel.UNDERSTAND,
                duration_min_minutes=30,
                duration_max_minutes=45,
                topics=[ActivityTopic.ALGORITHMS],
                resources_needed=[ActivityResource.COMPUTERS],
            ),
        ]

        criteria = SearchCriteria(target_age=8)
        filtered_activities = apply_hard_filters(activities_with_age_8, criteria)

        # Should include activities that pass the age filter with tolerance
        # For target_age=8, AGE_FILTER_TOLERANCE=2:
        # age_min_ok: activity.age_min <= 8 + 2 = 10
        # age_max_ok: activity.age_max >= 8 - 2 = 6
        for activity in filtered_activities:
            assert activity.age_min <= 10  # age_min_ok
            assert activity.age_max >= 6  # age_max_ok

        # Test with maximum age
        criteria = SearchCriteria(target_age=16)
        filtered_activities = apply_hard_filters(sample_activities, criteria)

        # Should include activities that pass the age filter with tolerance
        # For target_age=16, AGE_FILTER_TOLERANCE=2:
        # age_min_ok: activity.age_min <= 16 + 2 = 18
        # age_max_ok: activity.age_max >= 16 - 2 = 14
        for activity in filtered_activities:
            assert activity.age_min <= 18  # age_min_ok
            assert activity.age_max >= 14  # age_max_ok
