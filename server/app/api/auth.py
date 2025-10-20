"""Authentication API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask import current_app
from flask_openapi3 import Tag

from app.auth.decorators import admin_required, auth_required
from app.db.database import get_db_session
from app.db.models.user import UserRole
from app.services.email_service import EmailService
from app.services.user_service import UserService
from app.utils.pydantic_models import (
    CreateUserRequest,
    ErrorResponse,
    LoginRequest,
    LoginSuccessData,
    MessageResponse,
    PasswordResetRequest,
    RefreshTokenRequest,
    TeacherRegistrationRequest,
    TokenResponse,
    UpdateUserRequest,
    UserIdPath,
    UserResponse,
    VerificationCodeRequest,
    VerifyCodeRequest,
)
from app.utils.response_helpers import (
    error_response,
    success_response,
    unauthorized_response,
)

# Define API tag for OpenAPI
auth_tag = Tag(name="auth", description="Authentication and user management")


def _build_user_response(user):
    """Build standard user response object."""
    return {
        "id": user.id,
        "email": user.email,
        "first_name": user.first_name,
        "last_name": user.last_name,
        "role": user.role.value,
    }


def register_auth_routes(api):
    """Register authentication routes with Flask-OpenAPI3."""

    @api.post(
        "/api/auth/verification-code",
        tags=[auth_tag],
        responses={200: MessageResponse, 400: ErrorResponse, 422: ErrorResponse},
        summary="Request verification code",
        description="Send a verification code to the user's email address",
    )
    def request_verification_code(body: VerificationCodeRequest):
        """Request verification code for login."""
        try:
            from app.auth.auth_service import AuthService

            email = body.email
            db_session = get_db_session()
            user_service = UserService(db_session)
            auth_service = AuthService(user_service, db_session)

            # Generate verification code
            code = auth_service.generate_verification_code(email)
            if not code:
                # Log that user doesn't exist or is not a teacher, but return success to client
                current_app.logger.info(f"Verification code requested for non-existent user or non-teacher: {email}")
                return success_response({"message": "Verification code sent"})

            # Send verification code email
            email_service = EmailService()
            email_service.init_app(current_app)

            success = email_service.send_verification_code(email, code)
            if not success:
                return error_response("Temporary problem with email login", 500)

            return success_response({"message": "Verification code sent"})
        except Exception as e:
            return error_response(f"Failed to send verification code: {str(e)}", 500)

    @api.post(
        "/api/auth/verify",
        tags=[auth_tag],
        responses={200: LoginSuccessData, 400: ErrorResponse, 422: ErrorResponse},
        summary="Verify code and login",
        description="Verify the code and complete login process",
    )
    def verify_code(body: VerifyCodeRequest):
        """Verify code and login user."""
        try:
            from app.auth.auth_service import AuthService

            code = body.code
            email = body.email

            db_session = get_db_session()
            user_service = UserService(db_session)
            auth_service = AuthService(user_service, db_session)

            # Verify code
            user = auth_service.verify_verification_code(code, email)
            if not user:
                return unauthorized_response("Invalid or expired code")

            # Generate tokens
            tokens = auth_service.generate_tokens(user)

            return success_response(
                {
                    "user": _build_user_response(user),
                    "access_token": tokens["access_token"],
                    "refresh_token": tokens["refresh_token"],
                }
            )
        except Exception as e:
            return error_response(f"Failed to verify code: {str(e)}", 500)

    @api.post(
        "/api/auth/login",
        tags=[auth_tag],
        responses={200: LoginSuccessData, 400: ErrorResponse, 422: ErrorResponse},
        summary="Login with password",
        description="Login with email and password (admin or teacher)",
    )
    def login(body: LoginRequest):
        """Login with email and password."""
        try:
            from app.auth.auth_service import AuthService

            email = body.email
            password = body.password

            user_service = UserService(get_db_session())
            auth_service = AuthService(user_service, get_db_session())

            # Verify password credentials
            user = auth_service.verify_password_credentials(email, password)
            if not user:
                return unauthorized_response("Invalid credentials")

            # Generate tokens
            tokens = auth_service.generate_tokens(user)

            return success_response(
                {
                    "user": _build_user_response(user),
                    "access_token": tokens["access_token"],
                    "refresh_token": tokens["refresh_token"],
                }
            )
        except Exception as e:
            return error_response(f"Failed to login: {str(e)}", 500)

    @api.post(
        "/api/auth/admin/login",
        tags=[auth_tag],
        responses={200: LoginSuccessData, 400: ErrorResponse, 422: ErrorResponse},
        summary="Admin login",
        description="Login with admin credentials",
    )
    def admin_login(body: LoginRequest):
        """Admin login with email and password."""
        try:
            from app.auth.auth_service import AuthService

            email = body.email
            password = body.password

            user_service = UserService(get_db_session())
            auth_service = AuthService(user_service, get_db_session())

            # Verify admin credentials
            user = auth_service.verify_admin_credentials(email, password)
            if not user:
                return unauthorized_response("Invalid credentials")

            # Generate tokens
            tokens = auth_service.generate_tokens(user)

            return success_response(
                {
                    "user": _build_user_response(user),
                    "access_token": tokens["access_token"],
                    "refresh_token": tokens["refresh_token"],
                }
            )
        except Exception as e:
            return error_response(f"Failed to login: {str(e)}", 500)

    @api.get(
        "/api/auth/me",
        tags=[auth_tag],
        responses={200: UserResponse, 401: ErrorResponse},
        summary="Get current user",
        description="Get information about the currently authenticated user",
    )
    @auth_required
    def get_current_user():
        """Get current user information."""
        try:
            from flask import request

            user = request.user
            return success_response(
                {
                    "id": user.id,
                    "email": user.email,
                    "first_name": user.first_name,
                    "last_name": user.last_name,
                    "role": user.role.value,
                }
            )
        except Exception as e:
            return error_response(f"Failed to get user info: {str(e)}", 500)

    @api.post(
        "/api/auth/refresh",
        tags=[auth_tag],
        responses={200: TokenResponse, 400: ErrorResponse, 401: ErrorResponse},
        summary="Refresh token",
        description="Refresh the JWT access token using refresh token",
    )
    def refresh_token(body: RefreshTokenRequest):
        """Refresh JWT token."""
        try:
            from app.auth.auth_service import AuthService

            refresh_token = body.refresh_token

            user_service = UserService(get_db_session())
            auth_service = AuthService(user_service, get_db_session())

            # Verify refresh token
            user = auth_service.verify_refresh_token(refresh_token)
            if not user:
                return unauthorized_response("Invalid or expired refresh token")

            # Generate new tokens
            tokens = auth_service.generate_tokens(user)

            return success_response(
                {
                    "access_token": tokens["access_token"],
                    "refresh_token": tokens["refresh_token"],
                }
            )
        except Exception as e:
            return error_response(f"Failed to refresh token: {str(e)}", 500)

    @api.post(
        "/api/auth/logout",
        tags=[auth_tag],
        responses={200: MessageResponse, 401: ErrorResponse},
        summary="Logout",
        description="Logout the current user",
    )
    @auth_required
    def logout():
        """Logout user."""
        try:
            return success_response({"message": "Logged out successfully"})
        except Exception as e:
            return error_response(f"Failed to logout: {str(e)}", 500)

    @api.post(
        "/api/auth/register-teacher",
        tags=[auth_tag],
        responses={200: MessageResponse, 400: ErrorResponse, 409: ErrorResponse, 422: ErrorResponse},
        summary="Register teacher",
        description="Auto-register a new teacher with generated credentials",
    )
    def register_teacher(body: TeacherRegistrationRequest):
        """Register a new teacher with auto-generated credentials."""
        try:
            from app.utils.password_generator import PasswordGenerator

            email = body.email
            first_name = body.first_name
            last_name = body.last_name

            user_service = UserService(get_db_session())

            # Check if user already exists
            existing_user = user_service.get_user_by_email(email)
            if existing_user:
                return error_response("User with this email already exists", 409)

            # Create teacher user
            user = user_service.create_user(email, first_name, last_name, UserRole.TEACHER)

            # Generate secure password
            password = PasswordGenerator.generate_teacher_password()

            # Set password for teacher
            user_service.set_password(user.id, password)

            # Send credentials email
            email_service = EmailService()
            email_service.init_app(current_app)

            success = email_service.send_teacher_credentials(email, first_name, password)
            if not success:
                current_app.logger.warning(f"Failed to send credentials email to {email}, but user was created")

            return success_response(
                {
                    "message": "Teacher registered successfully. Credentials have been sent via email.",
                    "user": _build_user_response(user),
                },
                201,
            )
        except Exception as e:
            return error_response(f"Failed to register teacher: {str(e)}", 500)

    @api.post(
        "/api/auth/reset-password",
        tags=[auth_tag],
        responses={200: MessageResponse, 400: ErrorResponse, 404: ErrorResponse, 422: ErrorResponse},
        summary="Reset password",
        description="Reset password for a teacher",
    )
    def reset_password(body: PasswordResetRequest):
        """Reset password for a teacher."""
        try:
            from app.utils.password_generator import PasswordGenerator

            email = body.email

            user_service = UserService(get_db_session())

            # Check if user exists and is a teacher
            user = user_service.get_user_by_email(email)
            if not user or user.role != UserRole.TEACHER:
                return error_response("Teacher not found", 404)

            # Generate new secure password
            new_password = PasswordGenerator.generate_teacher_password()

            # Set new password
            user_service.set_password(user.id, new_password)

            # Send new credentials email
            email_service = EmailService()
            email_service.init_app(current_app)

            success = email_service.send_password_reset(email, user.first_name, new_password)
            if not success:
                current_app.logger.warning(f"Failed to send password reset email to {email}, but password was reset")

            return success_response(
                {"message": "Password reset successfully. New credentials have been sent via email."}
            )
        except Exception as e:
            return error_response(f"Failed to reset password: {str(e)}", 500)

    @api.get(
        "/api/auth/users",
        tags=[auth_tag],
        responses={200: MessageResponse, 401: ErrorResponse, 403: ErrorResponse, 500: ErrorResponse},
        summary="Get users",
        description="Get list of all users (admin only)",
    )
    @admin_required
    def get_users():
        """Get all users."""
        try:
            user_service = UserService(get_db_session())
            users = user_service.get_all_users()
            return success_response({"users": [_build_user_response(user) for user in users]})
        except Exception as e:
            return error_response(f"Failed to get users: {str(e)}", 500)

    @api.post(
        "/api/auth/users",
        tags=[auth_tag],
        responses={
            200: MessageResponse,
            401: ErrorResponse,
            403: ErrorResponse,
            409: MessageResponse,
            422: ErrorResponse,
            500: ErrorResponse,
        },
        summary="Create user",
        description="Create a new user (admin only)",
    )
    @admin_required
    def create_user(body: CreateUserRequest):
        """Create new user."""
        try:
            email = body.email
            first_name = body.first_name
            last_name = body.last_name
            role_str = body.role
            password = body.password

            role = UserRole(role_str)

            user_service = UserService(get_db_session())

            # Check if user already exists
            existing_user = user_service.get_user_by_email(email)
            if existing_user:
                return error_response("User with this email already exists", 409)

            # Create user
            user = user_service.create_user(email, first_name, last_name, role)

            # Handle password and email sending based on role
            if role == UserRole.ADMIN and password:
                # Set password for admin users (no email sent)
                user_service.set_password(user.id, password)
            elif role == UserRole.TEACHER:
                # Generate secure password for teachers and send email
                from app.utils.password_generator import PasswordGenerator

                teacher_password = PasswordGenerator.generate_teacher_password()
                user_service.set_password(user.id, teacher_password)

                # Send credentials email
                email_service = EmailService()
                email_service.init_app(current_app)

                success = email_service.send_teacher_credentials(email, first_name, teacher_password)
                if not success:
                    current_app.logger.warning(f"Failed to send credentials email to {email}, but user was created")

            return success_response(
                {"user": _build_user_response(user)},
                201,
            )
        except Exception as e:
            return error_response(f"Failed to create user: {str(e)}", 500)

    @api.put(
        "/api/auth/users/<int:user_id>",
        tags=[auth_tag],
        responses={
            200: MessageResponse,
            401: ErrorResponse,
            403: ErrorResponse,
            404: ErrorResponse,
            409: MessageResponse,
            422: ErrorResponse,
            500: ErrorResponse,
        },
        summary="Update user",
        description="Update user information (admin only)",
    )
    @admin_required
    def update_user(path: UserIdPath, body: UpdateUserRequest):
        """Update user."""
        try:

            user_id = path.user_id
            email = body.email
            first_name = body.first_name
            last_name = body.last_name
            role_str = body.role
            password = body.password

            user_service = UserService(get_db_session())
            user = user_service.get_user_by_id(user_id)

            if not user:
                return error_response("User not found", 404)

            # Update email if provided
            if email and email != user.email:
                # Check if email is already taken by another user
                existing_user = user_service.get_user_by_email(email)
                if existing_user and existing_user.id != user_id:
                    return error_response("Email already exists", 409)
                user.email = email

            # Update first_name if provided
            if first_name:
                user.first_name = first_name

            # Update last_name if provided
            if last_name:
                user.last_name = last_name

            # Update role if provided
            if role_str:
                role = UserRole(role_str)
                user.role = role

            # Update password if provided
            if password:
                user_service.set_password(user_id, password)

            # Save changes
            user_service.update_user(user)

            return success_response({"user": _build_user_response(user)})
        except Exception as e:
            return error_response(f"Failed to update user: {str(e)}", 500)

    @api.delete(
        "/api/auth/users/<int:user_id>",
        tags=[auth_tag],
        responses={
            200: MessageResponse,
            401: ErrorResponse,
            403: ErrorResponse,
            404: ErrorResponse,
            500: ErrorResponse,
        },
        summary="Delete user",
        description="Delete a user (admin only)",
    )
    @admin_required
    def delete_user(path: UserIdPath):
        """Delete user."""
        try:
            from flask import request

            user_id = path.user_id

            # Prevent admin from deleting themselves
            if user_id == request.user.id:
                return error_response("Cannot delete your own account")

            user_service = UserService(get_db_session())
            success = user_service.delete_user(user_id)

            if not success:
                return error_response("User not found", 404)

            return success_response({"message": "User deleted successfully"})
        except Exception as e:
            return error_response(f"Failed to delete user: {str(e)}", 500)
