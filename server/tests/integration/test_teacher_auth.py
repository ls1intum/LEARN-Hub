"""Integration tests for teacher authentication features."""

from unittest.mock import patch

from app.db.database import get_db_session
from app.db.models.user import UserRole
from app.services.user_service import UserService


class TestTeacherAuthentication:
    """Test teacher authentication functionality."""

    @patch("app.services.email_service.EmailService.send_teacher_credentials")
    def test_teacher_registration(self, mock_send_email, app, client):
        """Test teacher auto-registration endpoint."""
        # Mock email service to return success
        mock_send_email.return_value = True

        # Test data
        registration_data = {"email": "newteacher@example.com", "first_name": "New", "last_name": "Teacher"}

        # Make request
        response = client.post("/api/auth/register-teacher", json=registration_data)

        # Check response
        assert response.status_code == 201
        data = response.get_json()
        assert "Teacher registered successfully" in data["message"]
        assert data["user"]["email"] == "newteacher@example.com"
        assert data["user"]["role"] == "TEACHER"

        # Verify user was created in database
        db_session = get_db_session()
        user_service = UserService(db_session)
        user = user_service.get_user_by_email("newteacher@example.com")

        assert user is not None
        assert user.email == "newteacher@example.com"
        assert user.first_name == "New"
        assert user.last_name == "Teacher"
        assert user.role == UserRole.TEACHER
        assert user.password_hash is not None  # Password should be set

        # Verify email was called
        mock_send_email.assert_called_once()

    def test_teacher_registration_duplicate_email(self, app, client):
        """Test teacher registration with duplicate email."""
        # Create existing user
        db_session = get_db_session()
        user_service = UserService(db_session)
        user_service.create_user("existing@example.com", "Existing", "User", UserRole.TEACHER)

        # Try to register with same email
        registration_data = {"email": "existing@example.com", "first_name": "New", "last_name": "Teacher"}

        response = client.post("/api/auth/register-teacher", json=registration_data)

        # Should return conflict
        assert response.status_code == 409
        data = response.get_json()
        assert "already exists" in data["error"]

    def test_teacher_password_login(self, app, client):
        """Test teacher login with password."""
        # Create teacher with password
        db_session = get_db_session()
        user_service = UserService(db_session)
        user = user_service.create_user("teacher@example.com", "Test", "Teacher", UserRole.TEACHER)

        # Set password
        password = "TestPassword123!"
        user_service.set_password(user.id, password)

        # Login with password
        login_data = {"email": "teacher@example.com", "password": password}

        response = client.post("/api/auth/login", json=login_data)

        # Check response
        assert response.status_code == 200
        data = response.get_json()
        assert "access_token" in data
        assert "refresh_token" in data
        assert data["user"]["email"] == "teacher@example.com"
        assert data["user"]["role"] == "TEACHER"

    @patch("app.services.email_service.EmailService.send_password_reset")
    def test_teacher_password_reset(self, mock_send_email, app, client):
        """Test teacher password reset."""
        # Mock email service to return success
        mock_send_email.return_value = True

        # Create teacher
        db_session = get_db_session()
        user_service = UserService(db_session)
        user = user_service.create_user("teacher@example.com", "Test", "Teacher", UserRole.TEACHER)

        # Set initial password
        initial_password = "InitialPassword123!"
        user_service.set_password(user.id, initial_password)

        # Reset password
        reset_data = {"email": "teacher@example.com"}

        response = client.post("/api/auth/reset-password", json=reset_data)

        # Check response
        assert response.status_code == 200
        data = response.get_json()
        assert "Password reset successfully" in data["message"]

        # Verify old password no longer works
        login_data = {"email": "teacher@example.com", "password": initial_password}

        response = client.post("/api/auth/login", json=login_data)
        assert response.status_code == 401  # Should fail with old password

        # Verify email was called
        mock_send_email.assert_called_once()

    def test_password_reset_nonexistent_teacher(self, app, client):
        """Test password reset for non-existent teacher."""
        # Try to reset password for non-existent teacher
        reset_data = {"email": "nonexistent@example.com"}

        response = client.post("/api/auth/reset-password", json=reset_data)

        # Should return not found
        assert response.status_code == 404
        data = response.get_json()
        assert "Teacher not found" in data["error"]

    def test_password_reset_admin_user(self, app, client):
        """Test password reset for admin user (should fail)."""
        # Create admin user
        db_session = get_db_session()
        user_service = UserService(db_session)
        user_service.create_user("admin@example.com", "Admin", "User", UserRole.ADMIN)

        # Try to reset password for admin
        reset_data = {"email": "admin@example.com"}

        response = client.post("/api/auth/reset-password", json=reset_data)

        # Should return not found (admin is not a teacher)
        assert response.status_code == 404
        data = response.get_json()
        assert "Teacher not found" in data["error"]

    @patch("app.services.email_service.EmailService.send_teacher_credentials")
    def test_admin_creates_teacher_with_email(self, mock_send_email, app, client, auth_client):
        """Test admin creating teacher sends email with credentials."""
        # Mock email service to return success
        mock_send_email.return_value = True

        # Create teacher via admin endpoint
        teacher_data = {
            "email": "newteacher@example.com",
            "first_name": "New",
            "last_name": "Teacher",
            "role": "TEACHER",
        }

        response = auth_client.post("/api/auth/users", json=teacher_data)

        # Check response
        assert response.status_code == 201
        data = response.get_json()
        assert data["user"]["role"] == "TEACHER"

        # Verify user was created with password
        db_session = get_db_session()
        user_service = UserService(db_session)
        user = user_service.get_user_by_email("newteacher@example.com")

        assert user is not None
        assert user.password_hash is not None  # Password should be set

        # Verify email was called
        mock_send_email.assert_called_once()
