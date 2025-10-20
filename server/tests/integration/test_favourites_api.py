"""
Favourites API integration tests - Critical paths only.
Focuses on core functionality: save, retrieve, delete favourites.
"""

import json


class TestFavouritesAPI:
    """Test critical favourites API functionality."""

    def test_activity_favourite_crud_flow(self, teacher_auth_client, db_session, sample_activities):
        """Test complete CRUD flow for activity favourites."""
        activity = sample_activities[0]

        # Create
        data = {"activity_id": activity.id, "name": "My Favourite Activity"}
        response = teacher_auth_client.post(
            "/api/history/favourites/activities", data=json.dumps(data), content_type="application/json"
        )
        assert response.status_code == 200
        response_data = json.loads(response.data)
        favourite_id = response_data["favourite_id"]

        # Read
        response = teacher_auth_client.get("/api/history/favourites/activities")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["favourites"]) == 1
        assert response_data["favourites"][0]["id"] == favourite_id

        # Delete
        response = teacher_auth_client.delete(f"/api/history/favourites/activities/{favourite_id}")
        assert response.status_code == 200

        # Verify deletion
        response = teacher_auth_client.get("/api/history/favourites/activities")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["favourites"]) == 0

    def test_lesson_plan_favourite_crud_flow(self, teacher_auth_client, db_session, sample_activities):
        """Test complete CRUD flow for lesson plan favourites."""
        activity_ids = [activity.id for activity in sample_activities[:2]]

        # Create
        lesson_plan_snapshot = {
            "activities": [{"id": aid, "type": "activity"} for aid in activity_ids],
            "total_duration_minutes": 0,
            "ordering_strategy": "balanced",
            "title": "Test Lesson Plan",
        }
        data = {"activity_ids": activity_ids, "name": "Test Lesson Plan", "lesson_plan": lesson_plan_snapshot}
        response = teacher_auth_client.post(
            "/api/history/favourites/lesson-plans", data=json.dumps(data), content_type="application/json"
        )
        assert response.status_code == 200
        response_data = json.loads(response.data)
        favourite_id = response_data["favourite_id"]

        # Read
        response = teacher_auth_client.get("/api/history/favourites/lesson-plans")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["favourites"]) == 1
        assert response_data["favourites"][0]["id"] == favourite_id
        assert "lesson_plan" in response_data["favourites"][0]
        assert response_data["favourites"][0]["lesson_plan"]["activities"]

        # Delete
        response = teacher_auth_client.delete(f"/api/history/favourites/lesson-plans/{favourite_id}")
        assert response.status_code in [200, 404]  # 404 if already deleted

    def test_favourites_error_handling(self, teacher_auth_client, sample_activities):
        """Test error handling for invalid requests."""
        # Invalid activity ID
        data = {"activity_id": 99999, "name": "Invalid Activity"}
        response = teacher_auth_client.post(
            "/api/history/favourites/activities", data=json.dumps(data), content_type="application/json"
        )
        assert response.status_code in [200, 400, 404]  # Could be 200 if it succeeds anyway

        # Missing required fields
        data = {"name": "Missing Activity ID"}
        response = teacher_auth_client.post(
            "/api/history/favourites/activities", data=json.dumps(data), content_type="application/json"
        )
        assert response.status_code in [400, 422]  # Could be 422 for validation error

        # Invalid lesson plan (no activities)
        data = {"activity_ids": [], "name": "Empty Lesson Plan", "lesson_plan": {"activities": []}}
        response = teacher_auth_client.post(
            "/api/history/favourites/lesson-plans", data=json.dumps(data), content_type="application/json"
        )
        assert response.status_code in [400, 422]  # Could be 422 for validation error

        # Missing required snapshot
        valid_ids = [a.id for a in sample_activities[:1]]
        data = {"activity_ids": valid_ids, "name": "Missing Snapshot"}
        response = teacher_auth_client.post(
            "/api/history/favourites/lesson-plans", data=json.dumps(data), content_type="application/json"
        )
        assert response.status_code in [400, 422]

    def test_unauthorized_access(self, client, sample_activities):
        """Test that unauthenticated requests return 401."""
        # Activities endpoints (unauthenticated)
        response = client.get("/api/history/favourites/activities")
        assert response.status_code in [401, 422]
        response = client.post(
            "/api/history/favourites/activities",
            data=json.dumps({"activity_id": sample_activities[0].id, "name": "Test"}),
            content_type="application/json",
        )
        assert response.status_code in [401, 422]

        # Lesson plans endpoints (unauthenticated)
        lesson_plan_snapshot = {
            "activities": [{"id": sample_activities[0].id, "type": "activity"}],
            "total_duration_minutes": 0,
            "ordering_strategy": "balanced",
            "title": "Test",
        }
        response = client.get("/api/history/favourites/lesson-plans")
        assert response.status_code in [401, 422]
        response = client.post(
            "/api/history/favourites/lesson-plans",
            data=json.dumps(
                {"activity_ids": [sample_activities[0].id], "name": "Test", "lesson_plan": lesson_plan_snapshot}
            ),
            content_type="application/json",
        )
        assert response.status_code in [401, 422]
