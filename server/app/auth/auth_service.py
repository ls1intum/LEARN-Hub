from __future__ import annotations

from app.auth.jwt_service import JWTService
from app.db.models.user import User, UserRole
from app.services.user_service import UserService
from app.services.verification_service import VerificationService
from app.utils.password_utils import PasswordUtils


class AuthService:
    def __init__(self, user_service: UserService, db_session=None):
        self.user_service = user_service
        self.db_session = db_session

    def generate_verification_code(self, email: str) -> str | None:
        """Generate a 6-digit verification code for teacher authentication."""
        # Check if user exists and is a teacher
        user = self.user_service.get_user_by_email(email)
        if not user or user.role != UserRole.TEACHER:
            return None

        if not self.db_session:
            return None

        verification_service = VerificationService(self.db_session)
        return verification_service.generate_verification_code(user)

    def verify_verification_code(self, code: str, email: str) -> User | None:
        """Verify a 6-digit verification code and return the user."""
        # Get user from database
        user = self.user_service.get_user_by_email(email)
        if not user or user.role != UserRole.TEACHER:
            return None

        if not self.db_session:
            return None

        verification_service = VerificationService(self.db_session)

        # Verify the code
        if verification_service.verify_verification_code(code, user):
            return user
        else:
            # Increment attempts for failed verification
            verification_service.increment_attempts(code, user)
            return None

    def verify_admin_credentials(self, email: str, password: str) -> User | None:
        """Verify admin credentials and return the user if valid."""
        # Get user from database
        user = self.user_service.get_user_by_email(email)
        if not user or user.role != UserRole.ADMIN:
            return None

        # Verify password using user service
        if not self.user_service.verify_password(user.id, password):
            return None

        return user

    def verify_teacher_credentials(self, email: str, password: str) -> User | None:
        """Verify teacher credentials and return the user if valid."""
        # Get user from database
        user = self.user_service.get_user_by_email(email)
        if not user or user.role != UserRole.TEACHER:
            return None

        # Check if teacher has a password set
        if not user.password_hash:
            return None

        # Verify password using user service
        if not self.user_service.verify_password(user.id, password):
            return None

        return user

    def verify_password_credentials(self, email: str, password: str) -> User | None:
        """Verify password credentials for any user (admin or teacher)."""
        # Get user from database
        user = self.user_service.get_user_by_email(email)
        if not user:
            return None

        # Check if user has a password set
        if not user.password_hash:
            return None

        # Verify password using user service
        if not self.user_service.verify_password(user.id, password):
            return None

        return user

    def generate_tokens(self, user: User) -> dict:
        """Generate access and refresh tokens for a user."""
        return {
            "access_token": JWTService.generate_access_token(user),
            "refresh_token": JWTService.generate_refresh_token(user),
        }

    def verify_refresh_token(self, token: str) -> User | None:
        """Verify a refresh token and return the user."""
        payload = JWTService.verify_refresh_token(token)
        if not payload:
            return None

        # Get user from database
        user = self.user_service.get_user_by_id(payload["user_id"])
        if not user:
            return None

        return user

    def hash_password(self, password: str) -> str:
        """Hash a password using bcrypt."""
        return PasswordUtils.hash_password(password)

    def verify_password(self, password: str, hashed_password: str) -> bool:
        """Verify a password against its hash."""
        return PasswordUtils.verify_password(password, hashed_password)
