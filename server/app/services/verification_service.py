import secrets
from datetime import UTC, datetime, timedelta

from sqlalchemy.orm import Session

from app.db.models.user import User, VerificationCode


class VerificationService:
    """Service for managing 6-digit verification codes."""

    def __init__(self, db_session: Session):
        self.db_session = db_session

    def generate_verification_code(self, user: User) -> str:
        """Generate a 6-digit verification code for a user."""
        self._cleanup_expired_codes(user.id)

        code = f"{secrets.randbelow(1000000):06d}"
        expires_at = datetime.now(UTC) + timedelta(minutes=10)

        verification_code = VerificationCode(user_id=user.id, code=code, expires_at=expires_at, attempts=0, used="N")

        self.db_session.add(verification_code)
        self.db_session.commit()

        return code

    def verify_verification_code(self, code: str, user: User) -> bool:
        """Verify a 6-digit verification code for a user."""
        self._cleanup_expired_codes(user.id)

        verification_code = (
            self.db_session.query(VerificationCode)
            .filter(
                VerificationCode.user_id == user.id,
                VerificationCode.code == code,
                VerificationCode.used == "N",
                VerificationCode.expires_at > datetime.now(UTC),
            )
            .first()
        )

        if not verification_code:
            return False

        if verification_code.attempts >= 3:
            verification_code.used = "Y"
            self.db_session.commit()
            return False

        verification_code.used = "Y"
        self.db_session.commit()

        return True

    def increment_attempts(self, code: str, user: User) -> None:
        """Increment the attempt count for a verification code."""
        verification_code = (
            self.db_session.query(VerificationCode)
            .filter(VerificationCode.user_id == user.id, VerificationCode.code == code, VerificationCode.used == "N")
            .first()
        )

        if verification_code:
            verification_code.attempts += 1
            self.db_session.commit()

    def _cleanup_expired_codes(self, user_id: int) -> None:
        """Clean up expired verification codes for a user."""
        expired_codes = (
            self.db_session.query(VerificationCode)
            .filter(VerificationCode.user_id == user_id, VerificationCode.expires_at < datetime.now(UTC))
            .all()
        )

        for code in expired_codes:
            self.db_session.delete(code)

        self.db_session.commit()

    def cleanup_all_expired_codes(self) -> None:
        """Clean up all expired verification codes."""
        expired_codes = (
            self.db_session.query(VerificationCode).filter(VerificationCode.expires_at < datetime.now(UTC)).all()
        )

        for code in expired_codes:
            self.db_session.delete(code)

        self.db_session.commit()
