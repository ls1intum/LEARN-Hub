"""
Unit tests for transaction management (Unit of Work pattern).

Tests verify that services correctly handle the auto_commit parameter
and that complex operations maintain atomicity.
"""

from unittest.mock import Mock, patch

import pytest
from sqlalchemy.exc import IntegrityError

from app.db.models.user import User
from app.services.user_favourites_service import UserFavouritesService
from app.services.user_search_history_service import UserSearchHistoryService
from app.services.user_service import UserService
from app.services.verification_service import VerificationService


class TestVerificationServiceTransactions:
    """Test transaction management in VerificationService."""

    @pytest.fixture
    def verification_service(self):
        """Create a VerificationService instance with mocked database session."""
        mock_session = Mock()
        return VerificationService(mock_session), mock_session

    def test_delete_all_user_codes_with_auto_commit_true(self, verification_service):
        """Test delete_all_user_codes commits when auto_commit=True (default)."""
        service, mock_session = verification_service
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = 3

        # Act
        result = service.delete_all_user_codes(user_id)

        # Assert
        mock_session.query.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_session.commit.assert_called_once()
        assert result == 3

    def test_delete_all_user_codes_with_auto_commit_false(self, verification_service):
        """Test delete_all_user_codes does NOT commit when auto_commit=False."""
        service, mock_session = verification_service
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = 3

        # Act
        result = service.delete_all_user_codes(user_id, auto_commit=False)

        # Assert
        mock_session.query.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_session.commit.assert_not_called()
        assert result == 3


class TestUserSearchHistoryServiceTransactions:
    """Test transaction management in UserSearchHistoryService."""

    @pytest.fixture
    def search_history_service(self):
        """Create a UserSearchHistoryService instance with mocked database session."""
        mock_session = Mock()
        return UserSearchHistoryService(mock_session), mock_session

    def test_reset_user_search_history_with_auto_commit_true(self, search_history_service):
        """Test reset_user_search_history commits when auto_commit=True (default)."""
        service, mock_session = search_history_service
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = 5

        # Act
        result = service.reset_user_search_history(user_id)

        # Assert
        mock_session.query.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_session.commit.assert_called_once()
        assert result == 5

    def test_reset_user_search_history_with_auto_commit_false(self, search_history_service):
        """Test reset_user_search_history does NOT commit when auto_commit=False."""
        service, mock_session = search_history_service
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = 5

        # Act
        result = service.reset_user_search_history(user_id, auto_commit=False)

        # Assert
        mock_session.query.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_session.commit.assert_not_called()
        assert result == 5


class TestUserFavouritesServiceTransactions:
    """Test transaction management in UserFavouritesService."""

    @pytest.fixture
    def favourites_service(self):
        """Create a UserFavouritesService instance with mocked database session."""
        mock_session = Mock()
        return UserFavouritesService(mock_session), mock_session

    def test_reset_user_favourites_with_auto_commit_true(self, favourites_service):
        """Test reset_user_favourites commits when auto_commit=True (default)."""
        service, mock_session = favourites_service
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = 2

        # Act
        result = service.reset_user_favourites(user_id)

        # Assert
        mock_session.query.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_session.commit.assert_called_once()
        assert result == 2

    def test_reset_user_favourites_with_auto_commit_false(self, favourites_service):
        """Test reset_user_favourites does NOT commit when auto_commit=False."""
        service, mock_session = favourites_service
        user_id = 1

        # Mock query chain
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.delete.return_value = 2

        # Act
        result = service.reset_user_favourites(user_id, auto_commit=False)

        # Assert
        mock_session.query.assert_called_once()
        mock_filter.delete.assert_called_once()
        mock_session.commit.assert_not_called()
        assert result == 2


class TestUserServiceAtomicDeletion:
    """Test atomic user deletion in UserService."""

    @pytest.fixture
    def user_service(self):
        """Create a UserService instance with mocked database session."""
        mock_session = Mock()
        return UserService(mock_session), mock_session

    def test_delete_user_commits_only_once_at_end(self, user_service):
        """Test that delete_user commits only once at the end of all operations."""
        service, mock_session = user_service
        user_id = 1

        # Create a mock user
        mock_user = Mock(spec=User)
        mock_user.id = user_id

        # Mock get_user_by_id to return our mock user
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_user

        # Mock the delete operations on sub-services
        with (
            patch("app.services.user_service.VerificationService") as mock_verification_service,
            patch("app.services.user_service.UserSearchHistoryService") as mock_search_service,
            patch("app.services.user_service.UserFavouritesService") as mock_favourites_service,
        ):
            # Create mock instances
            mock_verification_instance = Mock()
            mock_search_instance = Mock()
            mock_favourites_instance = Mock()

            mock_verification_service.return_value = mock_verification_instance
            mock_search_service.return_value = mock_search_instance
            mock_favourites_service.return_value = mock_favourites_instance

            # Configure return values
            mock_verification_instance.delete_all_user_codes.return_value = 3
            mock_search_instance.reset_user_search_history.return_value = 5
            mock_favourites_instance.reset_user_favourites.return_value = 2

            # Act
            result = service.delete_user(user_id)

            # Assert
            assert result is True

            # Verify sub-services were called with auto_commit=False
            mock_verification_instance.delete_all_user_codes.assert_called_once_with(user_id, auto_commit=False)
            mock_search_instance.reset_user_search_history.assert_called_once_with(user_id, auto_commit=False)
            mock_favourites_instance.reset_user_favourites.assert_called_once_with(user_id, auto_commit=False)

            # Verify user was deleted
            mock_session.delete.assert_called_once_with(mock_user)

            # Verify commit was called exactly once at the end
            mock_session.commit.assert_called_once()

            # Verify rollback was never called
            mock_session.rollback.assert_not_called()

    def test_delete_user_rolls_back_on_error(self, user_service):
        """Test that delete_user rolls back all changes if an error occurs."""
        service, mock_session = user_service
        user_id = 1

        # Create a mock user
        mock_user = Mock(spec=User)
        mock_user.id = user_id

        # Mock get_user_by_id to return our mock user
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_user

        # Make the commit raise an error to simulate a failure
        mock_session.commit.side_effect = IntegrityError("Test error", params=None, orig=Exception("DB error"))

        # Mock the delete operations on sub-services
        with (
            patch("app.services.user_service.VerificationService") as mock_verification_service,
            patch("app.services.user_service.UserSearchHistoryService") as mock_search_service,
            patch("app.services.user_service.UserFavouritesService") as mock_favourites_service,
        ):
            # Create mock instances
            mock_verification_instance = Mock()
            mock_search_instance = Mock()
            mock_favourites_instance = Mock()

            mock_verification_service.return_value = mock_verification_instance
            mock_search_service.return_value = mock_search_instance
            mock_favourites_service.return_value = mock_favourites_instance

            # Configure return values
            mock_verification_instance.delete_all_user_codes.return_value = 3
            mock_search_instance.reset_user_search_history.return_value = 5
            mock_favourites_instance.reset_user_favourites.return_value = 2

            # Act & Assert
            with pytest.raises(IntegrityError):
                service.delete_user(user_id)

            # Verify rollback was called
            mock_session.rollback.assert_called_once()

    def test_delete_user_not_found(self, user_service):
        """Test that delete_user returns False when user is not found."""
        service, mock_session = user_service
        user_id = 999

        # Mock get_user_by_id to return None (user not found)
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = None

        # Act
        result = service.delete_user(user_id)

        # Assert
        assert result is False
        mock_session.commit.assert_not_called()
        mock_session.rollback.assert_not_called()

    def test_delete_user_wraps_non_integrity_errors(self, user_service):
        """Test that delete_user wraps non-IntegrityError exceptions."""
        service, mock_session = user_service
        user_id = 1

        # Create a mock user
        mock_user = Mock(spec=User)
        mock_user.id = user_id

        # Mock get_user_by_id to return our mock user
        mock_query = Mock()
        mock_filter = Mock()
        mock_session.query.return_value = mock_query
        mock_query.filter.return_value = mock_filter
        mock_filter.first.return_value = mock_user

        # Make the commit raise a generic error
        mock_session.commit.side_effect = ValueError("Unexpected error")

        # Mock the delete operations on sub-services
        with (
            patch("app.services.user_service.VerificationService") as mock_verification_service,
            patch("app.services.user_service.UserSearchHistoryService") as mock_search_service,
            patch("app.services.user_service.UserFavouritesService") as mock_favourites_service,
        ):
            # Create mock instances
            mock_verification_instance = Mock()
            mock_search_instance = Mock()
            mock_favourites_instance = Mock()

            mock_verification_service.return_value = mock_verification_instance
            mock_search_service.return_value = mock_search_instance
            mock_favourites_service.return_value = mock_favourites_instance

            # Act & Assert - should wrap ValueError in IntegrityError
            with pytest.raises(IntegrityError) as exc_info:
                service.delete_user(user_id)

            # Verify it's wrapped
            assert "Failed to delete user" in str(exc_info.value)

            # Verify rollback was called
            mock_session.rollback.assert_called_once()
