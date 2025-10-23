"""
Search History API integration tests - Critical paths only.
Focuses on core functionality: save, retrieve, delete search history.
"""

import json


class TestSearchHistoryAPI:
    """Test critical search history API functionality."""

    def test_search_history_crud_flow(self, teacher_auth_client, db_session):
        """Test complete CRUD flow for search history."""
        # Create search history entry by making a recommendation request
        search_criteria = {
            "topics": ["algorithms"],
            "bloom_levels": ["understand"],
            "mental_load": "medium",
            "physical_energy": "low",
        }

        # Simulate saving search history (this would normally happen in recommendation endpoint)
        from app.services.user_search_history_service import UserSearchHistoryService

        search_service = UserSearchHistoryService(db=db_session)
        search_entry = search_service.save_search_query(user_id=1, search_criteria=search_criteria)
        history_id = search_entry.id

        # Read - Get search history
        response = teacher_auth_client.get("/api/history/search")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["search_history"]) == 1
        assert response_data["search_history"][0]["id"] == history_id
        assert response_data["search_history"][0]["search_criteria"] == search_criteria

        # Delete - Delete specific search history entry
        response = teacher_auth_client.delete(f"/api/history/search/{history_id}")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert response_data["message"] == "Search history entry deleted successfully"

        # Verify deletion
        response = teacher_auth_client.get("/api/history/search")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["search_history"]) == 0

    def test_search_history_pagination(self, teacher_auth_client, db_session):
        """Test search history pagination."""
        # Create multiple search history entries
        from app.services.user_search_history_service import UserSearchHistoryService

        search_service = UserSearchHistoryService(db=db_session)

        for i in range(5):
            search_criteria = {"topic": f"test_{i}", "bloom_level": "understand"}
            search_service.save_search_query(user_id=1, search_criteria=search_criteria)

        # Test pagination
        response = teacher_auth_client.get("/api/history/search?limit=3&offset=0")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["search_history"]) == 3
        assert response_data["pagination"]["limit"] == 3
        assert response_data["pagination"]["offset"] == 0

        # Test second page
        response = teacher_auth_client.get("/api/history/search?limit=3&offset=3")
        assert response.status_code == 200
        response_data = json.loads(response.data)
        assert len(response_data["search_history"]) == 2  # Remaining entries

    def test_search_history_delete_not_found(self, teacher_auth_client):
        """Test deleting a search history entry that doesn't exist."""
        response = teacher_auth_client.delete("/api/history/search/999")
        assert response.status_code == 404
        response_data = json.loads(response.data)
        assert "not found" in response_data["error"].lower()

    def test_search_history_delete_unauthorized(self, teacher_auth_client, db_session):
        """Test that users can only delete their own search history."""
        # Create search history for user 1
        from app.services.user_search_history_service import UserSearchHistoryService

        search_service = UserSearchHistoryService(db=db_session)
        search_entry = search_service.save_search_query(user_id=1, search_criteria={"topic": "test"})
        history_id = search_entry.id

        # Try to delete as user 2 (this would require a different auth client)
        # For now, we'll test that the service properly checks ownership
        result = search_service.delete_search_history(history_id, user_id=2)  # Different user
        assert result is False  # Should fail due to ownership check

    def test_search_history_error_handling(self, teacher_auth_client):
        """Test search history error handling."""
        # Test invalid pagination parameters
        response = teacher_auth_client.get("/api/history/search?limit=-1")
        assert response.status_code == 400
        response_data = json.loads(response.data)
        assert "validation" in response_data["error"].lower() or "invalid" in response_data["error"].lower()

        # Test invalid offset
        response = teacher_auth_client.get("/api/history/search?offset=-1")
        assert response.status_code == 400
