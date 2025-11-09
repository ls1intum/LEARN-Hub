"""
Integration tests for atomic user deletion.

These tests verify that user deletion is truly atomic - all related data
is deleted in a single transaction, and failures cause complete rollback.
"""

import pytest
from sqlalchemy.exc import IntegrityError

from app.db.models.user import User, UserFavourites, UserRole, UserSearchHistory, VerificationCode
from app.services.user_favourites_service import UserFavouritesService
from app.services.user_search_history_service import UserSearchHistoryService
from app.services.user_service import UserService
from app.services.verification_service import VerificationService


class TestAtomicUserDeletion:
    """Integration tests for atomic user deletion."""

    @pytest.fixture
    def test_user(self, db_session, unique_email):
        """Create a test user with all types of related data."""
        # Create user
        user_service = UserService(db_session)
        user = user_service.create_user(unique_email, "Test", "User", role=UserRole.TEACHER)

        # Create verification codes
        verification_service = VerificationService(db_session)
        verification_service.generate_verification_code(user)
        verification_service.generate_verification_code(user)

        # Create search history
        search_history_service = UserSearchHistoryService(db_session)
        search_history_service.save_search_query(
            user.id,
            {"topic": "algorithms", "bloom_level": "understand"},
        )
        search_history_service.save_search_query(
            user.id,
            {"topic": "patterns", "bloom_level": "apply"},
        )
        search_history_service.save_search_query(
            user.id,
            {"topic": "abstraction", "bloom_level": "analyze"},
        )

        # Create favourites
        favourites_service = UserFavouritesService(db_session)
        favourites_service.save_activity_favourite(user.id, activity_id=1, name="Favorite Activity 1")
        favourites_service.save_lesson_plan_favourite(
            user.id,
            activity_ids=[1, 2, 3],
            name="Favorite Lesson Plan",
            lesson_plan_snapshot={"activities": [], "metadata": {}},
        )

        return user

    def test_delete_user_removes_all_related_data(self, db_session, test_user):
        """Test that deleting a user removes all related data."""
        user_id = test_user.id

        # Verify user and related data exist
        assert db_session.query(User).filter(User.id == user_id).first() is not None

        verification_count = db_session.query(VerificationCode).filter(VerificationCode.user_id == user_id).count()
        search_history_count = db_session.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).count()
        favourites_count = db_session.query(UserFavourites).filter(UserFavourites.user_id == user_id).count()

        assert verification_count == 2
        assert search_history_count == 3
        assert favourites_count == 2

        # Delete user
        user_service = UserService(db_session)
        result = user_service.delete_user(user_id)

        # Verify deletion was successful
        assert result is True

        # Verify user and all related data are gone
        assert db_session.query(User).filter(User.id == user_id).first() is None
        assert db_session.query(VerificationCode).filter(VerificationCode.user_id == user_id).count() == 0
        assert db_session.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).count() == 0
        assert db_session.query(UserFavourites).filter(UserFavourites.user_id == user_id).count() == 0

    def test_delete_user_nonexistent_returns_false(self, db_session):
        """Test that deleting a nonexistent user returns False."""
        user_service = UserService(db_session)
        result = user_service.delete_user(999999)

        assert result is False

    def test_delete_user_is_atomic_on_failure(self, db_session, test_user):
        """Test that user deletion is atomic - failures cause complete rollback."""
        user_id = test_user.id
        user_service = UserService(db_session)

        # Count records before deletion attempt
        initial_verification_count = (
            db_session.query(VerificationCode).filter(VerificationCode.user_id == user_id).count()
        )
        initial_search_history_count = (
            db_session.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).count()
        )
        initial_favourites_count = db_session.query(UserFavourites).filter(UserFavourites.user_id == user_id).count()

        assert initial_verification_count > 0
        assert initial_search_history_count > 0
        assert initial_favourites_count > 0

        # Force a failure by making the session commit fail
        # We'll simulate this by closing the session after operations start
        original_commit = db_session.commit

        def failing_commit():
            raise IntegrityError("Simulated database error", params=None, orig=Exception("Test error"))

        db_session.commit = failing_commit

        # Attempt to delete user - should raise exception
        with pytest.raises(IntegrityError):
            user_service.delete_user(user_id)

        # Restore original commit
        db_session.commit = original_commit

        # Rollback to clean state
        db_session.rollback()

        # Verify that ALL data is still present (atomic rollback)
        assert db_session.query(User).filter(User.id == user_id).first() is not None
        assert (
            db_session.query(VerificationCode).filter(VerificationCode.user_id == user_id).count()
            == initial_verification_count
        )
        assert (
            db_session.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).count()
            == initial_search_history_count
        )
        assert (
            db_session.query(UserFavourites).filter(UserFavourites.user_id == user_id).count()
            == initial_favourites_count
        )

    def test_delete_user_with_no_related_data(self, db_session, unique_email):
        """Test deleting a user with no related data."""
        # Create a user without any related data
        user_service = UserService(db_session)
        user = user_service.create_user(unique_email, "Test", "User", role=UserRole.TEACHER)
        user_id = user.id

        # Verify user exists
        assert db_session.query(User).filter(User.id == user_id).first() is not None

        # Delete user
        result = user_service.delete_user(user_id)

        # Verify deletion was successful
        assert result is True
        assert db_session.query(User).filter(User.id == user_id).first() is None

    def test_delete_user_commits_only_once(self, db_session, test_user, monkeypatch):
        """Test that delete_user commits only once at the end."""
        user_id = test_user.id
        user_service = UserService(db_session)

        # Track commit calls
        commit_count = 0
        original_commit = db_session.commit

        def tracking_commit():
            nonlocal commit_count
            commit_count += 1
            return original_commit()

        monkeypatch.setattr(db_session, "commit", tracking_commit)

        # Delete user
        result = user_service.delete_user(user_id)

        # Verify deletion was successful
        assert result is True

        # Verify commit was called exactly once
        assert commit_count == 1

    def test_concurrent_user_deletion_integrity(self, db_session, unique_email):
        """Test that multiple users can be deleted without affecting each other."""
        # Create multiple users with related data
        user_service = UserService(db_session)
        verification_service = VerificationService(db_session)
        search_history_service = UserSearchHistoryService(db_session)

        user1 = user_service.create_user(f"user1_{unique_email}", "User", "One", role=UserRole.TEACHER)
        user2 = user_service.create_user(f"user2_{unique_email}", "User", "Two", role=UserRole.TEACHER)
        user3 = user_service.create_user(f"user3_{unique_email}", "User", "Three", role=UserRole.TEACHER)

        # Add data to all users
        for user in [user1, user2, user3]:
            verification_service.generate_verification_code(user)
            search_history_service.save_search_query(user.id, {"topic": "test"})

        # Delete user2
        result = user_service.delete_user(user2.id)
        assert result is True

        # Verify user2 and its data are gone
        assert db_session.query(User).filter(User.id == user2.id).first() is None
        assert db_session.query(VerificationCode).filter(VerificationCode.user_id == user2.id).count() == 0

        # Verify user1 and user3 and their data still exist
        assert db_session.query(User).filter(User.id == user1.id).first() is not None
        assert db_session.query(User).filter(User.id == user3.id).first() is not None
        assert db_session.query(VerificationCode).filter(VerificationCode.user_id == user1.id).count() == 1
        assert db_session.query(VerificationCode).filter(VerificationCode.user_id == user3.id).count() == 1

    def test_delete_user_handles_large_dataset(self, db_session, unique_email):
        """Test deleting a user with large amounts of related data."""
        # Create a user with lots of related data
        user_service = UserService(db_session)
        user = user_service.create_user(unique_email, "Test", "User", role=UserRole.TEACHER)

        search_history_service = UserSearchHistoryService(db_session)
        favourites_service = UserFavouritesService(db_session)

        # Create many search history entries
        for i in range(50):
            search_history_service.save_search_query(user.id, {"topic": f"topic_{i}"})

        # Create many favourites
        for i in range(30):
            favourites_service.save_activity_favourite(user.id, activity_id=i + 1)

        user_id = user.id

        # Verify data exists
        assert db_session.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).count() == 50
        assert db_session.query(UserFavourites).filter(UserFavourites.user_id == user_id).count() == 30

        # Delete user
        result = user_service.delete_user(user_id)

        # Verify deletion was successful
        assert result is True
        assert db_session.query(User).filter(User.id == user_id).first() is None
        assert db_session.query(UserSearchHistory).filter(UserSearchHistory.user_id == user_id).count() == 0
        assert db_session.query(UserFavourites).filter(UserFavourites.user_id == user_id).count() == 0
