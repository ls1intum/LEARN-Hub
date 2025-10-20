from functools import wraps

from flask import abort, request

from app.auth.jwt_service import JWTService
from app.db.database import get_db_session
from app.db.models.user import UserRole
from app.services.user_service import UserService


def require_auth(roles: list[UserRole] | None = None):
    """
    Decorator to require authentication with optional role restrictions.

    Args:
        roles: List of allowed roles. If None, any authenticated user is allowed.
    """

    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            # Get token from Authorization header
            auth_header = request.headers.get("Authorization")
            if not auth_header or not auth_header.startswith("Bearer "):
                abort(401, description="Authentication required")

            token = auth_header.split(" ")[1]

            # Verify access token
            payload = JWTService.verify_access_token(token)
            if not payload:
                abort(401, description="Invalid or expired token")

            # Get user from database
            user_service = UserService(get_db_session())
            user = user_service.get_user_by_id(payload["user_id"])

            if not user:
                abort(401, description="Invalid user")

            # Check role restrictions if specified
            if roles and user.role not in roles:
                role_names = [role.value for role in roles]
                abort(403, description=f"Access denied. Required roles: {', '.join(role_names)}")

            request.user = user
            return f(*args, **kwargs)

        # Add security metadata for Flask-OpenAPI3
        if not hasattr(decorated_function, "__openapi_security__"):
            decorated_function.__openapi_security__ = [{"BearerAuth": []}]

        return decorated_function

    return decorator


# Convenience decorators for backward compatibility
def admin_required(f):
    """Decorator to require admin role."""
    return require_auth(roles=[UserRole.ADMIN])(f)


def teacher_required(f):
    """Decorator to require teacher role."""
    return require_auth(roles=[UserRole.TEACHER])(f)


def auth_required(f):
    """Decorator to require any authenticated user."""
    return require_auth()(f)


def maybe_auth(f):
    """Decorator that attaches request.user if a valid Bearer token is provided.

    Does not enforce authentication; proceeds anonymously if no/invalid token.
    """

    @wraps(f)
    def decorated_function(*args, **kwargs):
        auth_header = request.headers.get("Authorization")
        if auth_header and auth_header.startswith("Bearer "):
            token = auth_header.split(" ")[1]
            payload = JWTService.verify_access_token(token)
            if payload:
                user_service = UserService(get_db_session())
                user = user_service.get_user_by_id(payload.get("user_id"))
                if user:
                    request.user = user
        return f(*args, **kwargs)

    # Do not set security requirements for optional auth
    return decorated_function
