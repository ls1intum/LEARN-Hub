"""
Auth system integration tests - Critical paths only.
Focuses on core authentication functionality: login, admin verification, user creation.
"""

from datetime import UTC, datetime, timedelta

from app.auth.auth_service import AuthService
from app.db.models.user import UserRole, VerificationCode
from app.services.user_favourites_service import UserFavouritesService
from app.services.user_search_history_service import UserSearchHistoryService
from app.services.user_service import UserService


class TestAuthSystem:
    """Test critical auth system functionality."""

    def test_admin_credentials_verification_comprehensive(self, db_session):
        """Test admin credentials verification with success and failure cases."""
        user_service = UserService(db_session)
        auth_service = AuthService(user_service, db_session)

        # Create admin user with password
        admin_user = user_service.create_user("admin@test.com", "Admin", "User", role=UserRole.ADMIN)
        user_service.set_password(admin_user.id, "admin123")

        # Test successful verification
        user = auth_service.verify_admin_credentials("admin@test.com", "admin123")
        assert user is not None
        assert user.email == "admin@test.com"
        assert user.role == UserRole.ADMIN

        # Test wrong email
        user = auth_service.verify_admin_credentials("wrong@test.com", "admin123")
        assert user is None

        # Test wrong password
        user = auth_service.verify_admin_credentials("admin@test.com", "wrong_password")
        assert user is None

    def test_admin_user_creation_flow(self, db_session):
        """Test admin user creation and password setting."""
        user_service = UserService(db_session)
        auth_service = AuthService(user_service, db_session)

        # Create admin user directly first
        admin_user = user_service.create_user("admin@test.com", "Admin", "User", role=UserRole.ADMIN)
        user_service.set_password(admin_user.id, "admin123")

        # Test admin user verification
        user = auth_service.verify_admin_credentials("admin@test.com", "admin123")
        assert user is not None
        assert user.email == "admin@test.com"
        assert user.role == UserRole.ADMIN

    def test_teacher_credentials_verification(self, db_session):
        """Test teacher credentials verification."""
        user_service = UserService(db_session)
        auth_service = AuthService(user_service, db_session)

        # Create teacher user
        teacher_user = user_service.create_user("teacher@test.com", "Teacher", "User", role=UserRole.TEACHER)
        user_service.set_password(teacher_user.id, "teacher123")

        # Test successful verification
        user = auth_service.verify_teacher_credentials("teacher@test.com", "teacher123")
        assert user is not None
        assert user.email == "teacher@test.com"
        assert user.role == UserRole.TEACHER

        # Test wrong credentials
        user = auth_service.verify_teacher_credentials("teacher@test.com", "wrong_password")
        assert user is None

    def test_user_creation_and_management(self, db_session):
        """Test user creation and basic management operations."""
        user_service = UserService(db_session)

        # Create user
        user = user_service.create_user("test@example.com", "Test", "User", role=UserRole.TEACHER)
        assert user is not None
        assert user.email == "test@example.com"
        assert user.first_name == "Test"
        assert user.last_name == "User"
        assert user.role == UserRole.TEACHER

        # Test password setting
        user_service.set_password(user.id, "test123")

        # Test user retrieval
        retrieved_user = user_service.get_user_by_id(user.id)
        assert retrieved_user is not None
        assert retrieved_user.email == "test@example.com"

        # Test user deletion
        user_service.delete_user(user.id)
        deleted_user = user_service.get_user_by_id(user.id)
        assert deleted_user is None

    def test_auth_service_error_handling(self, db_session):
        """Test auth service error handling for edge cases."""
        user_service = UserService(db_session)
        auth_service = AuthService(user_service, db_session)

        # Test with non-existent user
        user = auth_service.verify_admin_credentials("nonexistent@test.com", "password")
        assert user is None

        # Test with empty credentials
        user = auth_service.verify_admin_credentials("", "")
        assert user is None

        user = auth_service.verify_admin_credentials("admin@test.com", "")
        assert user is None

    def test_user_deletion_with_verification_codes(self, db_session):
        """Test user deletion with non-expired verification codes."""
        user_service = UserService(db_session)

        # Create user
        user = user_service.create_user("test@example.com", "Test", "User", role=UserRole.TEACHER)

        # Create non-expired verification code
        expires_at = datetime.now(UTC) + timedelta(minutes=10)
        verification_code = VerificationCode(
            user_id=user.id, code="123456", expires_at=expires_at, attempts=0, used="N"
        )
        db_session.add(verification_code)
        db_session.commit()

        # Verify code exists
        codes_before = db_session.query(VerificationCode).filter(VerificationCode.user_id == user.id).all()
        assert len(codes_before) == 1

        # Delete user - should succeed after cleaning up verification codes
        success = user_service.delete_user(user.id)
        assert success is True

        # Verify user is deleted
        deleted_user = user_service.get_user_by_id(user.id)
        assert deleted_user is None

        # Verify verification codes are deleted
        codes_after = db_session.query(VerificationCode).filter(VerificationCode.user_id == user.id).all()
        assert len(codes_after) == 0

    def test_user_deletion_with_search_history(self, db_session):
        """Test user deletion with search history entries."""
        user_service = UserService(db_session)
        search_history_service = UserSearchHistoryService(db_session)

        # Create user
        user = user_service.create_user("test@example.com", "Test", "User", role=UserRole.TEACHER)

        # Create search history entries
        search_history_service.save_search_query(user.id, {"topic": "decomposition", "age_min": 5})
        search_history_service.save_search_query(user.id, {"topic": "patterns", "age_min": 7})

        # Verify history exists
        history_before = search_history_service.get_user_search_history(user.id)
        assert len(history_before) == 2

        # Delete user - should succeed after cleaning up search history
        success = user_service.delete_user(user.id)
        assert success is True

        # Verify user is deleted
        deleted_user = user_service.get_user_by_id(user.id)
        assert deleted_user is None

        # Verify search history is deleted
        history_after = search_history_service.get_user_search_history(user.id)
        assert len(history_after) == 0

    def test_user_deletion_with_favourites(self, db_session):
        """Test user deletion with favourite entries."""
        user_service = UserService(db_session)
        favourites_service = UserFavouritesService(db_session)

        # Create user
        user = user_service.create_user("test@example.com", "Test", "User", role=UserRole.TEACHER)

        # Create favourite entries
        favourites_service.save_activity_favourite(user.id, activity_id=1, name="Test Activity")
        favourites_service.save_lesson_plan_favourite(
            user.id, activity_ids=[1, 2, 3], name="Test Lesson Plan", lesson_plan_snapshot={"test": "data"}
        )

        # Verify favourites exist
        favourites_before = favourites_service.get_user_favourites(user.id)
        assert len(favourites_before) == 2

        # Delete user - should succeed after cleaning up favourites
        success = user_service.delete_user(user.id)
        assert success is True

        # Verify user is deleted
        deleted_user = user_service.get_user_by_id(user.id)
        assert deleted_user is None

        # Verify favourites are deleted
        favourites_after = favourites_service.get_user_favourites(user.id)
        assert len(favourites_after) == 0

    def test_user_deletion_with_all_related_data(self, db_session):
        """Test user deletion with all types of related data."""
        user_service = UserService(db_session)
        search_history_service = UserSearchHistoryService(db_session)
        favourites_service = UserFavouritesService(db_session)

        # Create user
        user = user_service.create_user("test@example.com", "Test", "User", role=UserRole.TEACHER)

        # Create verification code (non-expired)
        expires_at = datetime.now(UTC) + timedelta(minutes=10)
        verification_code = VerificationCode(
            user_id=user.id, code="123456", expires_at=expires_at, attempts=0, used="N"
        )
        db_session.add(verification_code)

        # Create search history
        search_history_service.save_search_query(user.id, {"topic": "decomposition"})

        # Create favourites
        favourites_service.save_activity_favourite(user.id, activity_id=1)

        db_session.commit()

        # Verify all data exists
        codes_before = db_session.query(VerificationCode).filter(VerificationCode.user_id == user.id).all()
        history_before = search_history_service.get_user_search_history(user.id)
        favourites_before = favourites_service.get_user_favourites(user.id)

        assert len(codes_before) == 1
        assert len(history_before) == 1
        assert len(favourites_before) == 1

        # Delete user - should succeed after cleaning up all related data
        success = user_service.delete_user(user.id)
        assert success is True

        # Verify user is deleted
        deleted_user = user_service.get_user_by_id(user.id)
        assert deleted_user is None

        # Verify all related data is deleted
        codes_after = db_session.query(VerificationCode).filter(VerificationCode.user_id == user.id).all()
        history_after = search_history_service.get_user_search_history(user.id)
        favourites_after = favourites_service.get_user_favourites(user.id)

        assert len(codes_after) == 0
        assert len(history_after) == 0
        assert len(favourites_after) == 0

    def test_user_deletion_with_expired_and_non_expired_codes(self, db_session):
        """Test user deletion with both expired and non-expired verification codes."""
        user_service = UserService(db_session)

        # Create user
        user = user_service.create_user("test@example.com", "Test", "User", role=UserRole.TEACHER)

        # Create expired verification code
        expires_at_expired = datetime.now(UTC) - timedelta(minutes=5)
        expired_code = VerificationCode(
            user_id=user.id, code="111111", expires_at=expires_at_expired, attempts=0, used="N"
        )
        db_session.add(expired_code)

        # Create non-expired verification code
        expires_at_valid = datetime.now(UTC) + timedelta(minutes=10)
        valid_code = VerificationCode(user_id=user.id, code="222222", expires_at=expires_at_valid, attempts=0, used="N")
        db_session.add(valid_code)

        db_session.commit()

        # Verify both codes exist
        codes_before = db_session.query(VerificationCode).filter(VerificationCode.user_id == user.id).all()
        assert len(codes_before) == 2

        # Delete user - should succeed after cleaning up all codes
        success = user_service.delete_user(user.id)
        assert success is True

        # Verify user is deleted
        deleted_user = user_service.get_user_by_id(user.id)
        assert deleted_user is None

        # Verify all codes are deleted (both expired and non-expired)
        codes_after = db_session.query(VerificationCode).filter(VerificationCode.user_id == user.id).all()
        assert len(codes_after) == 0
