"""
Auth system integration tests - Critical paths only.
Focuses on core authentication functionality: login, admin verification, user creation.
"""

from app.auth.auth_service import AuthService
from app.db.models.user import UserRole
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
