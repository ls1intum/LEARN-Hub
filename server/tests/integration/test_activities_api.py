"""
Activities API tests - Staff Engineer "More is Less" approach.
Focuses on critical business logic and error paths only.
"""

import json
from unittest.mock import patch


def test_get_activities_basic(auth_client, session, sample_activities):
    """Test basic activities listing with core functionality."""
    response = auth_client.get("/api/activities/")
    if response.status_code != 200:
        print(f"Response status: {response.status_code}")
        print(f"Response data: {response.data}")
    assert response.status_code == 200
    data = json.loads(response.data)

    assert len(data["activities"]) == len(sample_activities)
    assert "id" in data["activities"][0]
    assert "name" in data["activities"][0]


def test_get_activities_with_filters(auth_client, session, sample_activities):
    """Test activities filtering - core business logic."""
    response = auth_client.get("/api/activities/?format=unplugged&age_min=8&age_max=12")
    assert response.status_code == 200
    data = json.loads(response.data)

    for activity_data in data["activities"]:
        assert activity_data["format"] == "unplugged"
        assert activity_data["age_min"] <= 12
        assert activity_data["age_max"] >= 8


def test_get_activities_public_access(client):
    """Test activities are publicly accessible."""
    response = client.get("/api/activities/")
    assert response.status_code == 200


def test_get_recommendations_basic(auth_client, session, sample_activities):
    """Test basic recommendations - core business feature."""
    response = auth_client.get("/api/activities/recommendations?target_age=12&format=unplugged")
    if response.status_code != 200:
        print(f"Response status: {response.status_code}")
        print(f"Response data: {response.data}")
    assert response.status_code == 200
    data = json.loads(response.data)

    assert "activities" in data
    assert len(data["activities"]) > 0
    # Check that each recommendation has activities
    for rec in data["activities"]:
        assert "activities" in rec
        assert len(rec["activities"]) > 0


def test_get_recommendations_with_series(auth_client, session, sample_activities):
    """Test series recommendations - key business feature."""
    response = auth_client.get("/api/activities/recommendations?target_duration=50&allow_lesson_plans=true")
    assert response.status_code == 200
    data = json.loads(response.data)

    assert "activities" in data
    assert len(data["activities"]) > 0
    # Check that each recommendation has activities
    for rec in data["activities"]:
        assert "activities" in rec
        assert len(rec["activities"]) > 0


def test_get_recommendations_invalid_params(auth_client, session, sample_activities):
    """Test error handling for invalid parameters."""
    response = auth_client.get("/api/activities/recommendations?target_age=100&format=nonexistent")
    assert response.status_code == 422


def test_lesson_plan_generation(auth_client, session, sample_activities):
    """Test lesson plan generation - key business feature."""
    with patch("app.services.pdf_service.PDFService") as mock_service:
        mock_instance = mock_service.return_value
        mock_instance.get_lesson_plan_info.return_value = {
            "can_generate_lesson_plan": True,
            "total_activities": len(sample_activities),
        }
        mock_instance.generate_lesson_plan.return_value = b"%PDF-1.4 generated lesson plan"

        response = auth_client.post(
            "/api/activities/lesson-plan",
            json={
                "activities": [activity.to_dict() for activity in sample_activities],
                "search_criteria": {"target_age": 10},
                "breaks": [],
                "total_duration": 60,
            },
        )

        assert response.status_code == 200
        assert response.headers["Content-Type"] == "application/pdf"
