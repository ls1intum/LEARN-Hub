"""
Unit tests for UserFavouritesService.

Tests the service layer for managing user favourites (individual activities and lesson plans).
"""

from unittest.mock import Mock

import pytest

from app.db.models.user import UserFavourites
from app.services.user_favourites_service import UserFavouritesService


class TestUserFavouritesService:
    """Test cases for UserFavouritesService."""

    @pytest.fixture
    def mock_db_session(self):
        """Create a mock database session."""
        session = Mock()
        session.add = Mock()
        session.commit = Mock()
        session.refresh = Mock()
        session.query = Mock()
        session.delete = Mock()
        return session

    @pytest.fixture
    def favourites_service(self, mock_db_session):
        """Create a UserFavouritesService instance with mocked database session."""
        return UserFavouritesService(mock_db_session)

    def test_save_activity_favourite_success(self, favourites_service, mock_db_session):
        """Test successfully saving an activity favourite."""
        # Arrange
        user_id = 1
        activity_id = 123
        name = "My Favourite Activity"

        # Mock the database operations
        mock_favourite = Mock()
        mock_db_session.refresh.return_value = mock_favourite
        mock_favourite.id = 1

        # Act
        favourites_service.save_activity_favourite(user_id, activity_id, name)

        # Assert
        mock_db_session.add.assert_called_once()
        mock_db_session.commit.assert_called_once()
        mock_db_session.refresh.assert_called_once()

        # Verify the favourite object was created with correct attributes
        added_favourite = mock_db_session.add.call_args[0][0]
        assert isinstance(added_favourite, UserFavourites)
        assert added_favourite.user_id == user_id
        assert added_favourite.favourite_type == "activity"
        assert added_favourite.activity_id == activity_id
        assert added_favourite.name == name

    def test_save_activity_favourite_without_name(self, favourites_service, mock_db_session):
        """Test saving an activity favourite without a custom name."""
        # Arrange
        user_id = 1
        activity_id = 123

        # Act
        favourites_service.save_activity_favourite(user_id, activity_id)

        # Assert
        added_favourite = mock_db_session.add.call_args[0][0]
        assert added_favourite.name is None

    def test_save_lesson_plan_favourite_success(self, favourites_service, mock_db_session):
        """Test successfully saving a lesson plan favourite."""
        # Arrange
        user_id = 1
        activity_ids = [1, 2, 3, 4]
        name = "My Favourite Lesson Plan"

        # Act
        favourites_service.save_lesson_plan_favourite(user_id, activity_ids, name)

        # Assert
        mock_db_session.add.assert_called_once()
        mock_db_session.commit.assert_called_once()
        mock_db_session.refresh.assert_called_once()

        # Verify the favourite object was created with correct attributes
        added_favourite = mock_db_session.add.call_args[0][0]
        assert isinstance(added_favourite, UserFavourites)
        assert added_favourite.user_id == user_id
        assert added_favourite.favourite_type == "lesson_plan"
        assert added_favourite.activity_ids == activity_ids
        assert added_favourite.name == name

    def test_save_lesson_plan_favourite_without_name(self, favourites_service, mock_db_session):
        """Test saving a lesson plan favourite without a custom name."""
        # Arrange
        user_id = 1
        activity_ids = [1, 2, 3]

        # Act
        favourites_service.save_lesson_plan_favourite(user_id, activity_ids)

        # Assert
        added_favourite = mock_db_session.add.call_args[0][0]
        assert added_favourite.name is None

    def test_get_user_favourites(self, favourites_service, mock_db_session):
        """Test retrieving user favourites with pagination."""
        # Arrange
        user_id = 1
        limit = 10
        offset = 0

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_order_by = Mock()
        mock_offset = Mock()
        mock_limit = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.order_by.return_value = mock_order_by
        mock_order_by.offset.return_value = mock_offset
        mock_offset.limit.return_value = mock_limit
        mock_limit.all.return_value = []

        # Act
        result = favourites_service.get_user_favourites(user_id, limit, offset)

        # Assert
        mock_db_session.query.assert_called_once_with(UserFavourites)
        mock_query.filter.assert_called_once()
        mock_filter.order_by.assert_called_once()
        mock_order_by.offset.assert_called_once_with(offset)
        mock_offset.limit.assert_called_once_with(limit)
        assert result == []

    def test_get_favourite_by_id_success(self, favourites_service, mock_db_session):
        """Test retrieving a specific favourite by ID."""
        # Arrange
        favourite_id = 1
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_first = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_first

        # Act
        result = favourites_service.get_favourite_by_id(favourite_id, user_id)

        # Assert
        mock_db_session.query.assert_called_once_with(UserFavourites)
        mock_query.filter.assert_called_once()
        mock_filter.first.assert_called_once()
        assert result == mock_first

    def test_get_favourite_by_id_not_found(self, favourites_service, mock_db_session):
        """Test retrieving a favourite that doesn't exist."""
        # Arrange
        favourite_id = 999
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = None

        # Act
        result = favourites_service.get_favourite_by_id(favourite_id, user_id)

        # Assert
        assert result is None

    def test_delete_favourite_success(self, favourites_service, mock_db_session):
        """Test successfully deleting a favourite."""
        # Arrange
        favourite_id = 1
        user_id = 1

        # Mock the favourite exists
        mock_favourite = Mock()
        favourites_service.get_favourite_by_id = Mock(return_value=mock_favourite)

        # Act
        result = favourites_service.delete_favourite(favourite_id, user_id)

        # Assert
        favourites_service.get_favourite_by_id.assert_called_once_with(favourite_id, user_id)
        mock_db_session.delete.assert_called_once_with(mock_favourite)
        mock_db_session.commit.assert_called_once()
        assert result is True

    def test_delete_favourite_not_found(self, favourites_service, mock_db_session):
        """Test deleting a favourite that doesn't exist."""
        # Arrange
        favourite_id = 999
        user_id = 1

        # Mock the favourite doesn't exist
        favourites_service.get_favourite_by_id = Mock(return_value=None)

        # Act
        result = favourites_service.delete_favourite(favourite_id, user_id)

        # Assert
        favourites_service.get_favourite_by_id.assert_called_once_with(favourite_id, user_id)
        mock_db_session.delete.assert_not_called()
        mock_db_session.commit.assert_not_called()
        assert result is False

    def test_is_activity_favourited_true(self, favourites_service, mock_db_session):
        """Test checking if an activity is favourited (returns True)."""
        # Arrange
        user_id = 1
        activity_id = 123

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_first = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_first  # Favourite exists

        # Act
        result = favourites_service.is_activity_favourited(user_id, activity_id)

        # Assert
        assert result is True

    def test_is_activity_favourited_false(self, favourites_service, mock_db_session):
        """Test checking if an activity is favourited (returns False)."""
        # Arrange
        user_id = 1
        activity_id = 123

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = None  # No favourite exists

        # Act
        result = favourites_service.is_activity_favourited(user_id, activity_id)

        # Assert
        assert result is False

    def test_remove_activity_favourite_success(self, favourites_service, mock_db_session):
        """Test successfully removing an activity favourite."""
        # Arrange
        user_id = 1
        activity_id = 123

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_first = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_first  # Favourite exists

        # Act
        result = favourites_service.remove_activity_favourite(user_id, activity_id)

        # Assert
        mock_db_session.delete.assert_called_once_with(mock_first)
        mock_db_session.commit.assert_called_once()
        assert result is True

    def test_remove_activity_favourite_not_found(self, favourites_service, mock_db_session):
        """Test removing an activity favourite that doesn't exist."""
        # Arrange
        user_id = 1
        activity_id = 123

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = None  # No favourite exists

        # Act
        result = favourites_service.remove_activity_favourite(user_id, activity_id)

        # Assert
        mock_db_session.delete.assert_not_called()
        mock_db_session.commit.assert_not_called()
        assert result is False

    def test_get_user_activity_favourites(self, favourites_service, mock_db_session):
        """Test retrieving user's activity favourites."""
        # Arrange
        user_id = 1
        limit = 5
        offset = 0

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_order_by = Mock()
        mock_offset = Mock()
        mock_limit = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.order_by.return_value = mock_order_by
        mock_order_by.offset.return_value = mock_offset
        mock_offset.limit.return_value = mock_limit
        mock_limit.all.return_value = []

        # Act
        result = favourites_service.get_user_activity_favourites(user_id, limit, offset)

        # Assert
        mock_db_session.query.assert_called_once_with(UserFavourites)
        mock_query.filter.assert_called_once()
        mock_filter.order_by.assert_called_once()
        mock_order_by.offset.assert_called_once_with(offset)
        mock_offset.limit.assert_called_once_with(limit)
        assert result == []

    def test_get_user_lesson_plan_favourites(self, favourites_service, mock_db_session):
        """Test retrieving user's lesson plan favourites."""
        # Arrange
        user_id = 1
        limit = 5
        offset = 0

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_order_by = Mock()
        mock_offset = Mock()
        mock_limit = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.order_by.return_value = mock_order_by
        mock_order_by.offset.return_value = mock_offset
        mock_offset.limit.return_value = mock_limit
        mock_limit.all.return_value = []

        # Act
        result = favourites_service.get_user_lesson_plan_favourites(user_id, limit, offset)

        # Assert
        mock_db_session.query.assert_called_once_with(UserFavourites)
        mock_query.filter.assert_called_once()
        mock_filter.order_by.assert_called_once()
        mock_order_by.offset.assert_called_once_with(offset)
        mock_offset.limit.assert_called_once_with(limit)
        assert result == []

    def test_reset_user_favourites(self, favourites_service, mock_db_session):
        """Test resetting all favourites for a user."""
        # Arrange
        user_id = 1
        deleted_count = 3

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()

        mock_db_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = deleted_count

        # Act
        result = favourites_service.reset_user_favourites(user_id)

        # Assert
        mock_db_session.query.assert_called_once_with(UserFavourites)
        mock_query.filter.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_db_session.commit.assert_called_once()
        assert result == deleted_count
