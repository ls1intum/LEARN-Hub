"""Email templates for the LEARN-Hub application."""

from __future__ import annotations


def get_verification_code_template(code: str) -> tuple[str, str]:
    """
    Generate verification code email template.

    Args:
        code: The 6-digit verification code

    Returns:
        Tuple of (subject, body)
    """
    subject = "Your LEARN-Hub Login Code"

    body = f"""Hello!

You requested a login code for the LEARN-Hub.

Your 6-digit verification code is:

{code}

Enter this code on the login page to complete your authentication.

This code expires in 10 minutes and can only be used once.

If you didn't request this code, please ignore this email.

Best regards,
LEARN-Hub Team"""

    return subject, body


def get_teacher_credentials_template(email: str, first_name: str, password: str) -> tuple[str, str]:
    """
    Generate teacher credentials email template.

    Args:
        email: The teacher's email address
        first_name: The teacher's first name
        password: The generated password

    Returns:
        Tuple of (subject, body)
    """
    subject = "Welcome to LEARN-Hub - Your Account Credentials"

    body = f"""Hello {first_name}!

Welcome to LEARN-Hub! Your teacher account has been created.

Your login credentials are:
Email: {email}
Password: {password}

You can now log in to the LEARN-Hub using either:
1. Email verification (enter your email and request a code)
2. Password login (enter your email and password above)

If you have any questions, please contact your administrator.

Best regards,
LEARN-Hub Team"""

    return subject, body


def get_password_reset_template(email: str, first_name: str, new_password: str) -> tuple[str, str]:
    """
    Generate password reset email template.

    Args:
        email: The user's email address
        first_name: The user's first name
        new_password: The new generated password

    Returns:
        Tuple of (subject, body)
    """
    subject = "LEARN-Hub Password Reset - Your New Password"

    body = f"""Hello {first_name}!

Your password has been reset as requested.

Your new login credentials are:
Email: {email}
Password: {new_password}

You can now log in to the LEARN-Hub using either:
1. Email verification (enter your email and request a code)
2. Password login (enter your email and password above)

If you didn't request this password reset, please contact your administrator immediately.

Best regards,
LEARN-Hub Team"""

    return subject, body
