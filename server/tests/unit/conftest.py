"""
Unit test configuration and fixtures.

Unit tests should be fast and isolated, using mocks instead of real dependencies.
"""

import os
from unittest.mock import Mock, patch

import pytest

from app.utils.config import Config


@pytest.fixture(scope="function", autouse=True)
def setup_unit_test_env():
    """Set up environment for unit tests."""
    # Override environment variables for testing
    os.environ["GOOGLE_API_KEY"] = "test_key"
    os.environ["FLASK_SECRET_KEY"] = "test_flask_secret"
    os.environ["JWT_SECRET_KEY"] = "test_jwt_secret"
    os.environ["ADMIN_EMAIL"] = "admin@test.com"
    os.environ["ADMIN_PASSWORD"] = "admin123"
    os.environ["EMAIL_ADDRESS"] = "test@test.com"
    os.environ["EMAIL_USERNAME"] = "test@test.com"
    os.environ["EMAIL_PASSWORD"] = "test_password"
    os.environ["EMAIL_SENDER_NAME"] = "LEARN-Hub Test"
    os.environ["SMTP_SERVER"] = "smtp.test.com"
    os.environ["SMTP_PORT"] = "587"
    os.environ["PDF_STORAGE_PATH"] = "/tmp/test_pdfs"

    # Reset config
    Config._initialized = False
    yield
    # Cleanup after test
    Config._initialized = False


@pytest.fixture
def mock_db_session():
    """Mock database session for unit tests."""
    with patch("app.db.database.get_db_session") as mock:
        mock_session = Mock()
        mock.return_value = mock_session
        yield mock_session


@pytest.fixture
def mock_user_service():
    """Mock user service for unit tests."""
    with patch("app.services.user_service.UserService") as mock:
        yield mock


@pytest.fixture
def mock_email_service():
    """Mock email service for unit tests."""
    with patch("app.services.email_service.EmailService") as mock:
        mock_instance = Mock()
        mock_instance.send_verification_code.return_value = True
        mock.return_value = mock_instance
        yield mock_instance


@pytest.fixture
def mock_flask_mail():
    """Mock Flask-Mail for unit tests."""
    with patch("flask_mail.Mail") as mock:
        mock_instance = Mock()
        mock_instance.send.return_value = None
        mock.return_value = mock_instance
        yield mock_instance
