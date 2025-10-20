"""Secure password generation utility."""

from __future__ import annotations

import secrets
import string


class PasswordGenerator:
    """Generate secure passwords for teacher accounts."""

    # Characters that are easy to distinguish and type
    ALPHANUMERIC_CHARS = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    SYMBOL_CHARS = "!@#$%^&*"

    @classmethod
    def generate_teacher_password(cls) -> str:
        """
        Generate a secure password for teacher accounts.

        Format: 15 alphanumeric characters + 1 symbol
        No ambiguous characters (0, O, I, l, 1)

        Returns:
            str: A secure password
        """
        # Generate 15 alphanumeric characters
        alphanumeric = "".join(secrets.choice(cls.ALPHANUMERIC_CHARS) for _ in range(15))

        # Generate 1 symbol
        symbol = secrets.choice(cls.SYMBOL_CHARS)

        # Combine and shuffle
        password_chars = list(alphanumeric + symbol)
        secrets.SystemRandom().shuffle(password_chars)

        return "".join(password_chars)

    @classmethod
    def is_secure_password(cls, password: str) -> bool:
        """
        Check if a password meets security requirements.

        Args:
            password: Password to check

        Returns:
            bool: True if password meets requirements
        """
        if len(password) < 8:
            return False

        # Check for at least one lowercase letter
        if not any(c.islower() for c in password):
            return False

        # Check for at least one uppercase letter
        if not any(c.isupper() for c in password):
            return False

        # Check for at least one digit
        if not any(c.isdigit() for c in password):
            return False

        # Check for at least one symbol
        if not any(c in string.punctuation for c in password):
            return False

        return True
