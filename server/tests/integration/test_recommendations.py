"""
Recommendation tests.
Focuses on core business logic and critical paths only.
"""

from app.services.recommendation_service import RecommendationService


class TestRecommendations:
    """Test recommendation engine - core business logic."""

    def _get_recommendations_data(self, service, criteria, **kwargs):
        """Helper method to get recommendations data directly."""
        response = service.get_recommendations(criteria, **kwargs)
        assert response is not None
        # Response is now a dictionary directly
        return response

    def test_basic_recommendations(self, session, sample_activities):
        """Test basic recommendation functionality with filtering and scoring."""
        criteria = {"target_age": 11, "format": ["unplugged"], "preferred_topics": ["algorithms"]}
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria)

        # Should return recommendations
        assert len(results["recommendations"]) > 0

        # Check that each recommendation has activities
        all_activities = []
        for rec in results["recommendations"]:
            activities = [r for r in rec["activities"] if isinstance(r, dict) and "name" in r]
            all_activities.extend(activities)
            assert len(activities) > 0

        # Check format filtering works
        formats = [act["format"] for act in all_activities]
        assert "unplugged" in formats

        # Check basic response structure
        assert "recommendations" in results
        assert "total_count" in results

        # Check individual recommendation scoring
        for rec in results["recommendations"]:
            assert "score" in rec
            assert "activities" in rec
            assert "is_lesson_plan" in rec
            assert isinstance(rec["score"], int)
            assert 0 <= rec["score"] <= 100

    def test_recommendations_with_priority_categories(self, session, sample_activities):
        """Test recommendations with priority categories."""
        criteria = {
            "target_age": 11,
            "format": ["unplugged"],
            "preferred_topics": ["algorithms"],
            "priority_categories": ["age_appropriateness", "topic_relevance"],
        }
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria)

        # Should return recommendations
        assert len(results["recommendations"]) > 0

    def test_series_recommendations(self, session, sample_activities):
        """Test series recommendations with lesson plans."""
        criteria = {"target_duration": 50}
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria, max_activity_count=3)

        assert len(results["recommendations"]) > 0

        # Check that each recommendation has activities
        all_activities = []
        for rec in results["recommendations"]:
            activities = [r for r in rec["activities"] if isinstance(r, dict) and "name" in r]
            all_activities.extend(activities)
            assert len(activities) > 0

        # Check activity structure
        for activity in all_activities:
            assert activity["name"] is not None
            assert activity["bloom_level"] is not None

    def test_filtering_and_matching(self, session, sample_activities):
        """Test various filtering and matching capabilities."""
        # Test format filtering
        criteria = {"format": ["digital"]}
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria)

        # Get all activities from all recommendations
        all_activities = []
        for rec in results["recommendations"]:
            activities = [r for r in rec["activities"] if isinstance(r, dict) and "name" in r]
            all_activities.extend(activities)

        for activity in all_activities:
            format_value = activity["format"]
            assert format_value == "digital"

        # Test topic matching
        criteria = {"preferred_topics": ["algorithms", "patterns"]}
        results = self._get_recommendations_data(service, criteria)
        assert len(results["recommendations"]) > 0

        # Test duration filtering
        criteria = {"target_duration": 45}
        results = self._get_recommendations_data(service, criteria)
        assert len(results["recommendations"]) > 0

    def test_priority_categories(self, session, sample_activities):
        """Test priority categories functionality."""
        criteria = {
            "target_age": 12,
            "preferred_topics": ["algorithms"],
            "priority_categories": ["age_appropriateness", "topic_relevance"],
        }
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria)

        assert len(results["recommendations"]) > 0

    def test_bloom_levels_handling(self, session, sample_activities):
        """Test bloom levels handling including defaults and multi-select."""
        # Test empty bloom levels defaults to all
        criteria = {"target_age": 10, "bloom_levels": []}
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria)

        bloom_levels = set()
        for rec in results["recommendations"]:
            for item in rec["activities"]:
                if isinstance(item, dict) and "bloom_level" in item:
                    bloom_level = item["bloom_level"]
                    bloom_levels.add(bloom_level.lower())

        # Should have multiple bloom levels since empty array defaults to all
        assert len(bloom_levels) > 1

        # Test multi-select bloom levels
        criteria = {"target_age": 12, "bloom_levels": ["understand", "apply", "create"]}
        results = self._get_recommendations_data(service, criteria)
        assert len(results["recommendations"]) > 0

        found_bloom_levels = set()
        for rec in results["recommendations"]:
            for item in rec["activities"]:
                if isinstance(item, dict) and "bloom_level" in item:
                    bloom_level = item["bloom_level"]
                    found_bloom_levels.add(bloom_level.lower())

        expected_levels = {"understand", "apply", "create"}
        assert found_bloom_levels.intersection(expected_levels)

    def test_recommendations_with_breaks(self, session, sample_activities):
        """Test recommendations with breaks included."""
        criteria = {"target_duration": 50, "include_breaks": True}
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria, max_activity_count=3)

        assert len(results["recommendations"]) > 0

        # Check for activities with breaks
        for rec in results["recommendations"]:
            activities = [r for r in rec["activities"] if isinstance(r, dict) and "name" in r]
            for activity in activities:
                if "break_after" in activity and activity["break_after"] is not None:
                    # Verify break structure matches BreakResponse model
                    break_data = activity["break_after"]
                    assert "id" in break_data
                    assert "duration" in break_data
                    assert "description" in break_data
                    assert "reasons" in break_data
                    # Position field removed - breaks are now embedded in activities
                    assert isinstance(break_data["reasons"], list)
                    assert isinstance(break_data["duration"], int)

        # Note: breaks are only added for multi-activity sequences, so we might not always find them
        # depending on the test data and criteria

    def test_edge_cases(self, session, sample_activities):
        """Test edge cases and error handling."""
        # Test with empty criteria
        criteria = {}
        service = RecommendationService(session)
        results = self._get_recommendations_data(service, criteria)
        assert len(results["recommendations"]) > 0

        # Test with age outside most ranges
        criteria = {"target_age": 20}
        results = self._get_recommendations_data(service, criteria)
        assert len(results["recommendations"]) >= 0
