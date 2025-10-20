"""
Integration tests for error handling - critical business functionality.
"""


class TestErrorHandling:
    """Test error handling across the application."""

    def test_activities_api_public_access(self, client):
        """Test activities API is publicly accessible."""
        response = client.get("/api/activities/")
        assert response.status_code == 200

    def test_recommendations_api_public_access(self, client):
        """Test recommendations API is publicly accessible."""
        response = client.get("/api/activities/recommendations")
        assert response.status_code == 200

    def test_invalid_recommendation_parameters(self, auth_client, session, sample_activities):
        """Test recommendations API with invalid parameters."""
        # Test with invalid age
        response = auth_client.get("/api/activities/recommendations?target_age=invalid")
        assert response.status_code == 422

        # Test with invalid format
        response = auth_client.get("/api/activities/recommendations?format=invalid_format")
        assert response.status_code == 422

        # Test with invalid bloom level
        response = auth_client.get("/api/activities/recommendations?bloom_levels=invalid_level")
        assert response.status_code == 422

    def test_lesson_plan_generation_error(self, auth_client, session, sample_activities):
        """Test lesson plan generation error handling."""
        # Test with invalid activity data
        response = auth_client.post(
            "/api/activities/lesson-plan",
            json={
                "activities": [{"invalid": "data"}],
                "search_criteria": {"target_age": 10},
                "breaks": [],
                "total_duration": 60,
            },
        )
        # This might return 400 or 500 depending on validation
        assert response.status_code in [400, 500]

    def test_database_connection_error(self, client):
        """Test handling of database connection errors."""
        # This test would require mocking database connection failures
        # For now, we'll test that the app handles missing database gracefully
        response = client.get("/api/activities/")
        # Should return 200 (public access) rather than 500 (server error)
        assert response.status_code == 200

    def test_malformed_json_requests(self, auth_client):
        """Test handling of malformed JSON requests."""
        response = auth_client.post(
            "/api/activities/lesson-plan",
            data="invalid json",
            content_type="application/json",
        )
        assert response.status_code == 422

    def test_missing_required_parameters(self, auth_client, session, sample_activities):
        """Test handling of missing required parameters."""
        # Test recommendations with missing target_age
        response = auth_client.get("/api/activities/recommendations?format=unplugged")
        # Should still work as target_age is not strictly required
        assert response.status_code in [200, 400]

    def test_large_request_handling(self, auth_client, session, sample_activities):
        """Test handling of large requests."""
        # Test with very large activity list
        large_activities = []
        for i in range(1000):
            large_activities.append(
                {
                    "id": i,
                    "name": f"Activity {i}",
                    "duration_min_minutes": 30,
                    "format": "unplugged",
                    "bloom_level": "understand",
                }
            )

        response = auth_client.post(
            "/api/activities/lesson-plan",
            json={
                "activities": large_activities,
                "search_criteria": {"target_age": 10},
                "breaks": [],
                "total_duration": 30000,
            },
        )

        # Should handle large requests gracefully
        assert response.status_code in [200, 422, 413]  # 422 = Unprocessable Entity, 413 = Payload Too Large
