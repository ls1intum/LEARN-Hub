"""
Main test configuration and shared fixtures.

This file contains shared fixtures that can be used across both unit and integration tests.
Specific fixtures are now organized in their respective conftest.py files:
- tests/unit/conftest.py - Unit test fixtures (mocks, isolated components)
- tests/integration/conftest.py - Integration test fixtures (database, API clients)
"""

import os
import uuid
from unittest.mock import Mock, patch

import pytest

from app.utils.config import Config


@pytest.fixture(scope="function", autouse=True)
def setup_test_env():
    """Set up basic environment for all tests."""
    # Override environment variables for testing
    os.environ["GOOGLE_API_KEY"] = "test_key"
    os.environ["DEV_SECRET_KEY"] = "test_dev_secret"
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

    # Reset config singleton
    Config._instance = None
    Config._initialized = False
    yield
    # Cleanup after test
    Config._instance = None
    Config._initialized = False


@pytest.fixture(scope="function")
def unique_email():
    """Generate a unique email address for each test."""
    return f"test_{uuid.uuid4().hex[:8]}@example.com"


@pytest.fixture(scope="function")
def mock_email_service():
    """Mock email service to avoid sending actual emails during testing."""
    with patch("app.services.email_service.EmailService") as mock:
        mock_instance = Mock()
        mock_instance.send_verification_code.return_value = True

        mock.return_value = mock_instance
        yield mock_instance


@pytest.fixture(scope="function")
def mock_flask_mail():
    """Mock Flask-Mail to avoid email configuration issues during testing."""
    with patch("flask_mail.Mail") as mock:
        mock_instance = Mock()
        mock_instance.send.return_value = None
        mock.return_value = mock_instance
        yield mock_instance
