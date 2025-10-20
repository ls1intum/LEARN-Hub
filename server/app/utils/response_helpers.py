"""Standardized API response helpers for consistent formatting across all endpoints."""

from typing import Any

from flask import jsonify


def success_response(data: Any = None, status_code: int = 200) -> tuple:
    """Create a clean success response that returns data directly.

    For 2xx responses, return data directly without wrapper objects.
    """
    if data is None:
        return jsonify({}), status_code
    return jsonify(data), status_code


def error_response(message: str, status_code: int = 400, error_code: str = None) -> tuple:
    """Create a clean error response with error object.

    For 4xx/5xx responses, return {"error": "error message"} with appropriate HTTP status code.
    """
    response = {"error": message}
    if error_code:
        response["error_code"] = error_code
    return jsonify(response), status_code


def validation_error_response(errors: dict[str, list[str]], message: str = "Validation failed") -> tuple:
    """Create a validation error response."""
    return jsonify({"error": message, "validation_errors": errors}), 400


def not_found_response(resource: str = "Resource") -> tuple:
    """Create a not found response."""
    return error_response(f"{resource} not found", 404)


def unauthorized_response(message: str = "Authentication required") -> tuple:
    """Create an unauthorized response."""
    return error_response(message, 401)


def forbidden_response(message: str = "Access denied") -> tuple:
    """Create a forbidden response."""
    return error_response(message, 403)


def server_error_response(message: str = "Internal server error") -> tuple:
    """Create a server error response."""
    return error_response(message, 500)
