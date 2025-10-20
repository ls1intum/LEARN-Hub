"""Unit tests for verification service."""

from unittest.mock import Mock, patch

from app.db.models.user import User
from app.services.verification_service import VerificationService


class TestVerificationService:
    """Test VerificationService functionality."""

    def setup_method(self):
        """Set up test fixtures."""
        self.mock_session = Mock()
        self.service = VerificationService(self.mock_session)

    def test_generate_verification_code(self):
        """Test verification code generation."""
        user = User(id=1, email="test@example.com")

        with patch.object(self.service, "_cleanup_expired_codes"):
            result = self.service.generate_verification_code(user)

            assert len(result) == 6
            assert result.isdigit()
            self.mock_session.add.assert_called_once()
            self.mock_session.commit.assert_called()

    def test_verify_verification_code_success(self):
        """Test successful verification code verification."""
        user = User(id=1, email="test@example.com")
        code = "123456"

        mock_code = Mock()
        mock_code.attempts = 0
        mock_code.used = "N"

        with patch.object(self.service, "_cleanup_expired_codes"):
            self.mock_session.query.return_value.filter.return_value.first.return_value = mock_code

            result = self.service.verify_verification_code(code, user)

            assert result is True
            assert mock_code.used == "Y"
            self.mock_session.commit.assert_called()

    def test_verify_verification_code_not_found(self):
        """Test verification code verification when code not found."""
        user = User(id=1, email="test@example.com")
        code = "123456"

        with patch.object(self.service, "_cleanup_expired_codes"):
            self.mock_session.query.return_value.filter.return_value.first.return_value = None

            result = self.service.verify_verification_code(code, user)

            assert result is False

    def test_verify_verification_code_too_many_attempts(self):
        """Test verification code verification with too many attempts."""
        user = User(id=1, email="test@example.com")
        code = "123456"

        mock_code = Mock()
        mock_code.attempts = 3
        mock_code.used = "N"

        with patch.object(self.service, "_cleanup_expired_codes"):
            self.mock_session.query.return_value.filter.return_value.first.return_value = mock_code

            result = self.service.verify_verification_code(code, user)

            assert result is False
            assert mock_code.used == "Y"
            self.mock_session.commit.assert_called()

    def test_increment_attempts(self):
        """Test incrementing verification code attempts."""
        user = User(id=1, email="test@example.com")
        code = "123456"

        mock_code = Mock()
        mock_code.attempts = 1

        self.mock_session.query.return_value.filter.return_value.first.return_value = mock_code

        self.service.increment_attempts(code, user)

        assert mock_code.attempts == 2
        self.mock_session.commit.assert_called()

    def test_cleanup_expired_codes(self):
        """Test cleanup of expired verification codes."""
        user_id = 1
        expired_code = Mock()

        self.mock_session.query.return_value.filter.return_value.all.return_value = [expired_code]

        self.service._cleanup_expired_codes(user_id)

        self.mock_session.delete.assert_called_with(expired_code)
        self.mock_session.commit.assert_called()

    def test_cleanup_all_expired_codes(self):
        """Test cleanup of all expired verification codes."""
        expired_code1 = Mock()
        expired_code2 = Mock()

        self.mock_session.query.return_value.filter.return_value.all.return_value = [expired_code1, expired_code2]

        self.service.cleanup_all_expired_codes()

        assert self.mock_session.delete.call_count == 2
        self.mock_session.commit.assert_called()
