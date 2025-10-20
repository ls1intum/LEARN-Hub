from datetime import UTC, datetime, timedelta

import jwt

from app.db.models.user import User
from app.utils.config import Config


class JWTService:
    """Service for handling JWT token generation and verification."""

    @staticmethod
    def generate_token(user: User, token_type: str) -> str:
        """Generate a JWT token for a user."""
        config = Config.get_instance()

        # Determine expiration based on token type
        if token_type == "access":
            expires_seconds = config.jwt_access_token_expires
        elif token_type == "refresh":
            expires_seconds = config.jwt_refresh_token_expires
        else:
            raise ValueError(f"Invalid token type: {token_type}")

        payload = {
            "user_id": user.id,
            "email": user.email,
            "role": user.role.value,
            "exp": datetime.now(UTC) + timedelta(seconds=expires_seconds),
            "iat": datetime.now(UTC),
            "type": token_type,
        }
        return jwt.encode(payload, config.jwt_secret_key, algorithm="HS256")

    @staticmethod
    def generate_access_token(user: User) -> str:
        """Generate a short-lived access token."""
        return JWTService.generate_token(user, "access")

    @staticmethod
    def generate_refresh_token(user: User) -> str:
        """Generate a long-lived refresh token."""
        return JWTService.generate_token(user, "refresh")

    @staticmethod
    def verify_token(token: str, expected_type: str = None) -> dict | None:
        """Verify a JWT token and return the payload if valid."""
        try:
            # Use global config instance
            config = Config.get_instance()
            payload = jwt.decode(token, config.jwt_secret_key, algorithms=["HS256"])

            # Check if token is expired
            if datetime.now(UTC) > datetime.fromtimestamp(payload["exp"], tz=UTC):
                return None

            # Check token type if specified
            if expected_type and payload.get("type") != expected_type:
                return None

            return payload

        except jwt.InvalidTokenError:
            return None

    @staticmethod
    def verify_access_token(token: str) -> dict | None:
        """Verify an access token."""
        return JWTService.verify_token(token, "access")

    @staticmethod
    def verify_refresh_token(token: str) -> dict | None:
        """Verify a refresh token."""
        return JWTService.verify_token(token, "refresh")

    @staticmethod
    def extract_user_info_from_token(token: str) -> dict | None:
        """Extract user information from a valid token."""
        payload = JWTService.verify_token(token)
        if not payload:
            return None

        return {
            "user_id": payload.get("user_id"),
            "email": payload.get("email"),
            "role": payload.get("role"),
        }
