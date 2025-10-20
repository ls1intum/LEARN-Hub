"""
Auth Decorators tests - Staff Engineer "More is Less" approach.
Focuses on core security functionality only.
"""

from unittest.mock import Mock, patch

import pytest
from flask import Flask
from werkzeug.exceptions import Forbidden, Unauthorized

from app.auth.decorators import admin_required, auth_required, teacher_required
from app.db.models.user import UserRole


class TestAuthDecorators:
    """Test the JWT-based authentication decorators - core security."""

    @pytest.fixture
    def app(self):
        """Create a minimal Flask app for testing decorators."""
        app = Flask(__name__)
        app.config["TESTING"] = True
        app.config["SECRET_KEY"] = "test_secret_key"
        return app

    @pytest.fixture
    def mock_user_service(self):
        """Mock user service."""
        with patch("app.auth.decorators.UserService") as mock:
            yield mock

    @pytest.fixture
    def mock_db_session(self):
        """Mock database session."""
        with patch("app.auth.decorators.get_db_session") as mock:
            yield mock

    def test_auth_required_success(self, app, mock_user_service, mock_db_session):
        """Test auth_required decorator with valid user."""
        mock_user = Mock()
        mock_user.role = UserRole.TEACHER
        mock_user_service.return_value.get_user_by_id.return_value = mock_user

        with patch("app.auth.decorators.JWTService.verify_access_token") as mock_verify:
            mock_verify.return_value = {"user_id": 1}

            with app.test_request_context(headers={"Authorization": "Bearer valid_token"}):

                @auth_required
                def test_function():
                    return "success"

                result = test_function()
                assert result == "success"

    def test_auth_required_no_token(self, app):
        """Test auth_required decorator without token."""
        with app.test_request_context():

            @auth_required
            def test_function():
                return "success"

            with pytest.raises(Unauthorized):
                test_function()

    def test_auth_required_invalid_token(self, app):
        """Test auth_required decorator with invalid token."""
        with patch("app.auth.decorators.JWTService.verify_access_token") as mock_verify:
            mock_verify.return_value = None

            with app.test_request_context(headers={"Authorization": "Bearer invalid_token"}):

                @auth_required
                def test_function():
                    return "success"

                with pytest.raises(Unauthorized):
                    test_function()

    def test_admin_required_success(self, app, mock_user_service, mock_db_session):
        """Test admin_required decorator with valid admin user."""
        mock_user = Mock()
        mock_user.role = UserRole.ADMIN
        mock_user_service.return_value.get_user_by_id.return_value = mock_user

        with patch("app.auth.decorators.JWTService.verify_access_token") as mock_verify:
            mock_verify.return_value = {"user_id": 1}

            with app.test_request_context(headers={"Authorization": "Bearer valid_token"}):

                @admin_required
                def test_function():
                    return "success"

                result = test_function()
                assert result == "success"

    def test_admin_required_teacher_user(self, app, mock_user_service, mock_db_session):
        """Test admin_required decorator with teacher user (should fail)."""
        mock_user = Mock()
        mock_user.role = UserRole.TEACHER
        mock_user_service.return_value.get_user_by_id.return_value = mock_user

        with patch("app.auth.decorators.JWTService.verify_access_token") as mock_verify:
            mock_verify.return_value = {"user_id": 1}

            with app.test_request_context(headers={"Authorization": "Bearer valid_token"}):

                @admin_required
                def test_function():
                    return "success"

                with pytest.raises(Forbidden):
                    test_function()

    def test_teacher_required_success(self, app, mock_user_service, mock_db_session):
        """Test teacher_required decorator with valid teacher user."""
        mock_user = Mock()
        mock_user.role = UserRole.TEACHER
        mock_user_service.return_value.get_user_by_id.return_value = mock_user

        with patch("app.auth.decorators.JWTService.verify_access_token") as mock_verify:
            mock_verify.return_value = {"user_id": 1}

            with app.test_request_context(headers={"Authorization": "Bearer valid_token"}):

                @teacher_required
                def test_function():
                    return "success"

                result = test_function()
                assert result == "success"
