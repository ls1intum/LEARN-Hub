"""
Unit tests for UserSearchHistoryService.
Focuses on core functionality: save, retrieve, delete search history.
"""

from unittest.mock import Mock

import pytest

from app.services.user_search_history_service import UserSearchHistoryService


class TestUserSearchHistoryService:
    """Test cases for UserSearchHistoryService."""

    @pytest.fixture
    def search_history_service(self, mock_db_session):
        """Create a UserSearchHistoryService instance with mocked database session."""
        return UserSearchHistoryService(db=mock_db_session)

    def test_save_search_query_success(self, search_history_service, mock_db_session):
        """Test successfully saving a search query."""
        # Arrange
        user_id = 1
        search_criteria = {"topic": "algorithms", "bloom_level": "understand"}

        # Act
        result = search_history_service.save_search_query(user_id, search_criteria)

        # Assert
        mock_db_session.add.assert_called_once()
        mock_db_session.commit.assert_called_once()
        mock_db_session.refresh.assert_called_once()
        assert result is not None

    def test_get_user_search_history_success(self, search_history_service, mock_db_session):
        """Test retrieving user search history."""
        # Arrange
        user_id = 1
        limit = 10
        offset = 0

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_order = Mock()
        mock_offset = Mock()
        mock_limit = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.order_by.return_value = mock_order
        mock_order.offset.return_value = mock_offset
        mock_offset.limit.return_value = mock_limit
        mock_limit.all.return_value = []

        # Act
        result = search_history_service.get_user_search_history(user_id, limit, offset)

        # Assert
        mock_db_session.query.assert_called_once()
        mock_query.filter.assert_called_once()
        mock_filter.order_by.assert_called_once()
        mock_order.offset.assert_called_once_with(offset)
        mock_offset.limit.assert_called_once_with(limit)
        mock_limit.all.assert_called_once()
        assert result == []

    def test_get_search_history_by_id_success(self, search_history_service, mock_db_session):
        """Test retrieving a specific search history entry by ID."""
        # Arrange
        history_id = 1
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_first = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_first

        # Act
        result = search_history_service.get_search_history_by_id(history_id, user_id)

        # Assert
        mock_db_session.query.assert_called_once()
        mock_query.filter.assert_called_once()
        mock_filter.first.assert_called_once()
        assert result == mock_first

    def test_get_search_history_by_id_not_found(self, search_history_service, mock_db_session):
        """Test retrieving a search history entry that doesn't exist."""
        # Arrange
        history_id = 999
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = None

        # Act
        result = search_history_service.get_search_history_by_id(history_id, user_id)

        # Assert
        assert result is None

    def test_delete_search_history_success(self, search_history_service, mock_db_session):
        """Test successfully deleting a search history entry."""
        # Arrange
        history_id = 1
        user_id = 1

        # Mock the history entry exists
        mock_history = Mock()
        search_history_service.get_search_history_by_id = Mock(return_value=mock_history)

        # Act
        result = search_history_service.delete_search_history(history_id, user_id)

        # Assert
        search_history_service.get_search_history_by_id.assert_called_once_with(history_id, user_id)
        mock_db_session.delete.assert_called_once_with(mock_history)
        mock_db_session.commit.assert_called_once()
        assert result is True

    def test_delete_search_history_not_found(self, search_history_service, mock_db_session):
        """Test deleting a search history entry that doesn't exist."""
        # Arrange
        history_id = 999
        user_id = 1

        # Mock the history entry doesn't exist
        search_history_service.get_search_history_by_id = Mock(return_value=None)

        # Act
        result = search_history_service.delete_search_history(history_id, user_id)

        # Assert
        search_history_service.get_search_history_by_id.assert_called_once_with(history_id, user_id)
        mock_db_session.delete.assert_not_called()
        mock_db_session.commit.assert_not_called()
        assert result is False

    def test_reset_user_search_history_success(self, search_history_service, mock_db_session):
        """Test successfully resetting all search history for a user."""
        # Arrange
        user_id = 1
        deleted_count = 5

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = deleted_count

        # Act
        result = search_history_service.reset_user_search_history(user_id)

        # Assert
        mock_db_session.query.assert_called_once()
        mock_query.filter.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_db_session.commit.assert_called_once()
        assert result == deleted_count
