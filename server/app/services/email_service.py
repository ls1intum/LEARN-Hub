import os
import smtplib
from email.mime.text import MIMEText

from flask import current_app

from app.utils.config import Config
from app.utils.email_templates import (
    get_password_reset_template,
    get_teacher_credentials_template,
    get_verification_code_template,
)


class EmailService:
    def __init__(self, app=None):
        """Initialize the email service."""
        pass

    def init_app(self, app):
        """Initialize the email service with Flask app (for compatibility)."""
        pass

    def _send_email(self, to_email: str, subject: str, body: str) -> bool:
        """Send a simple email using SMTP with STARTTLS."""
        # Get config from global instance
        config = Config.get_instance()

        # Skip email sending in test environments
        if os.environ.get("PYTEST_CURRENT_TEST") or "test" in os.environ.get("_", ""):
            current_app.logger.info(f"Test mode: Would send email to {to_email}: {subject}")
            return True

        try:
            # Create simple message
            msg = MIMEText(body)
            msg["From"] = config.EMAIL_SENDER_FORMATTED
            msg["To"] = to_email
            msg["Subject"] = subject

            # Send email (port 587 uses STARTTLS)
            with smtplib.SMTP(config.smtp_server, config.smtp_port) as server:
                server.starttls()  # Enable encryption
                server.login(config.email_username, config.email_password)
                server.sendmail(config.email_address, to_email, msg.as_string())

            current_app.logger.info(f"✅ Email sent successfully to {to_email}")
            return True

        except Exception as e:
            current_app.logger.error(f"Failed to send email to {to_email}: {str(e)}")
            # In development, log the email content for debugging
            current_app.logger.warning(f"Email content for {to_email}: {subject}\n{body}")
            return False

    def send_verification_code(self, email: str, code: str) -> bool:
        """Send a 6-digit verification code email to the user."""
        subject, body = get_verification_code_template(code)
        return self._send_email(to_email=email, subject=subject, body=body)

    def send_teacher_credentials(self, email: str, first_name: str, password: str) -> bool:
        """Send initial credentials to a newly registered teacher."""
        subject, body = get_teacher_credentials_template(email, first_name, password)
        return self._send_email(to_email=email, subject=subject, body=body)

    def send_password_reset(self, email: str, first_name: str, new_password: str) -> bool:
        """Send new password after password reset."""
        subject, body = get_password_reset_template(email, first_name, new_password)
        return self._send_email(to_email=email, subject=subject, body=body)
