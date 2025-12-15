"""
Integration test configuration and fixtures.

Integration tests use real database connections and test multiple components together.
"""

import os
import uuid
from unittest.mock import Mock, patch

import pytest

from app.db.database import get_db_session
from app.db.models.user import UserRole
from app.main import get_app
from app.services.user_service import UserService


@pytest.fixture(scope="function")
def app():
    """Create a test Flask application."""
    # Use true in-memory SQLite for each test - no file creation

    # Override environment variables for testing
    os.environ["SQLALCHEMY_DATABASE_URI"] = "sqlite:///:memory:?cache=private&check_same_thread=false"
    os.environ["DATABASE_URL"] = "sqlite:///:memory:?cache=private&check_same_thread=false"
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
    os.environ["PDF_PATH"] = "/tmp/test_pdfs"

    # Reset global database state
    global engine
    engine = None

    # Reset config completely
    from app.utils.config import Config

    Config._initialized = False
    Config._instance = None

    # Force reset of database module globals
    import app.db.database as db_module

    db_module.engine = None
    db_module.db_session = None

    app = get_app()

    with app.app_context():
        # Force fresh database for each test
        from app.db.database import Base, get_engine

        # Import all models to ensure they're registered with Base
        from app.db.models.activity import Activity  # noqa: F401
        from app.db.models.user import PDFDocument, User, UserFavourites, UserSearchHistory  # noqa: F401

        # Double-check we're using true in-memory database
        current_uri = os.environ.get("SQLALCHEMY_DATABASE_URI", "")
        if not current_uri.startswith("sqlite:///:memory:"):
            raise RuntimeError(f"Tests must use true in-memory SQLite database, got: {current_uri}")

        # Verify the actual database URI being used
        from app.utils.config import Config

        actual_uri = Config.get_instance().SQLALCHEMY_DATABASE_URI
        if not actual_uri.startswith("sqlite:///:memory:"):
            raise RuntimeError(f"Config is not using SQLite, got: {actual_uri}")

        engine = get_engine()
        # Clear any existing tables (ignore errors if tables don't exist)
        try:
            Base.metadata.drop_all(bind=engine)
        except Exception:
            pass  # Ignore errors if tables don't exist
        Base.metadata.create_all(bind=engine)  # Create fresh tables

        yield app

        # Clean up database after test (still within app context)
        try:
            # Remove scoped session to close thread-local sessions
            session = get_db_session()
            session.remove()
        except Exception:
            pass  # Ignore cleanup errors

        # Dispose engine to close all connections
        try:
            engine.dispose()
        except Exception:
            pass  # Ignore disposal errors


@pytest.fixture(scope="function")
def client(app):
    """Create a test client."""
    return app.test_client()


@pytest.fixture(scope="function")
def db_session(app):
    """Get a database session for testing."""
    with app.app_context():
        session = get_db_session()
        yield session
        # Clean up after each test - close without rollback to avoid errors
        try:
            session.close()
        except Exception:
            pass  # Ignore close errors


@pytest.fixture(scope="function")
def session(db_session):
    """Alias for db_session to maintain compatibility with existing tests."""
    return db_session


@pytest.fixture(scope="function")
def auth_client(client, db_session, unique_email):
    """Create an authenticated test client with admin user using JWT."""
    from app.auth.jwt_service import JWTService

    # Create an admin user with unique email
    user_service = UserService(db_session)
    admin = user_service.create_user(unique_email, "Test", "Admin", role=UserRole.ADMIN)

    # Generate JWT token
    access_token = JWTService.generate_access_token(admin)

    # Set authorization header
    client.environ_base["HTTP_AUTHORIZATION"] = f"Bearer {access_token}"

    return client


@pytest.fixture(scope="function")
def teacher_auth_client(client, db_session, unique_email):
    """Create an authenticated test client with teacher user using JWT."""
    from app.auth.jwt_service import JWTService

    # Create a teacher user with unique email
    user_service = UserService(db_session)
    teacher = user_service.create_user(unique_email, "Test", "Teacher", role=UserRole.TEACHER)

    # Generate JWT token
    access_token = JWTService.generate_access_token(teacher)

    # Set authorization header
    client.environ_base["HTTP_AUTHORIZATION"] = f"Bearer {access_token}"

    return client


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


@pytest.fixture(scope="function")
def sample_activities(db_session):
    """Create sample activities for testing using test_data.py."""
    from app.db.models.user import PDFDocument, UserRole

    from .test_data import create_test_activities

    # Create a teacher user for activities with a unique email
    teacher_email = f"teacher_{uuid.uuid4().hex[:8]}@example.com"
    user_service = UserService(db_session)
    user_service.create_user(teacher_email, "Test", "Teacher", role=UserRole.TEACHER)

    # Create a mock PDF document for test activities
    mock_pdf = PDFDocument(
        id=1,
        filename="test_activity.pdf",
        file_path="/tmp/test_pdfs/test_activity.pdf",
        file_size=1024,
        extracted_fields={"name": "Test Activity"},
        confidence_score="0.95",
        extraction_quality="high",
    )
    db_session.add(mock_pdf)
    db_session.commit()

    # Get activities from test_data and add them to the session
    activities = create_test_activities()
    for activity in activities:
        db_session.add(activity)

    db_session.commit()
    return activities
