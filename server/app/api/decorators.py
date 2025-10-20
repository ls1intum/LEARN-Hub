"""API decorators for validation and standardization."""

from __future__ import annotations

from collections.abc import Callable
from functools import wraps
from typing import Any

from flask import request
from pydantic import ValidationError as PydanticValidationError

from app.utils.response_helpers import error_response
from app.utils.validation import ActivityFormValidator, RecommendationFormValidator, ValidationError


def validate_request(validator_class: type) -> Callable:
    """
    Decorator to validate request data using a validator class.

    Args:
        validator_class: The validator class to use (e.g., ActivityFormValidator)
    """

    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            try:
                # Get request data
                if request.is_json:
                    data = request.get_json()
                elif request.form:
                    data = request.form.to_dict()
                else:
                    data = request.args.to_dict()

                # Validate data
                if validator_class == ActivityFormValidator:
                    validated_data = validator_class.validate_activity_data(data)
                elif validator_class == RecommendationFormValidator:
                    validated_data = validator_class.validate_recommendation_criteria(data)
                else:
                    raise ValueError(f"Unknown validator class: {validator_class}")

                # Add validated data to kwargs
                kwargs["validated_data"] = validated_data

                return func(*args, **kwargs)

            except ValidationError as e:
                return error_response(f"Validation error: {e.message}", 400)
            except Exception as e:
                return error_response(f"Request validation failed: {str(e)}", 400)

        return wrapper

    return decorator


def validate_pydantic_request(model_class: type) -> Callable:
    """
    Decorator to validate request data using a Pydantic model.

    Args:
        model_class: The Pydantic model class to use for validation
    """

    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            try:
                # Get request data
                if request.is_json:
                    data = request.get_json()
                elif request.form:
                    data = request.form.to_dict()
                else:
                    data = request.args.to_dict()

                # Validate data using Pydantic model
                validated_data = model_class(**data)

                # Add validated data to kwargs
                kwargs["validated_data"] = validated_data.model_dump()

                return func(*args, **kwargs)

            except PydanticValidationError as e:
                # Format Pydantic validation errors
                error_messages = []
                for error in e.errors():
                    field = " -> ".join(str(loc) for loc in error["loc"])
                    message = error["msg"]
                    error_messages.append(f"{field}: {message}")

                return error_response(f"Validation error: {'; '.join(error_messages)}", 400)
            except Exception as e:
                return error_response(f"Request validation failed: {str(e)}", 400)

        return wrapper

    return decorator


def validate_query_params(validator_class: type) -> Callable:
    """
    Decorator to validate query parameters using a validator class.

    Args:
        validator_class: The validator class to use
    """

    def decorator(func: Callable) -> Callable:
        @wraps(func)
        def wrapper(*args: Any, **kwargs: Any) -> Any:
            try:
                # Get query parameters
                params = request.args.to_dict()

                # Validate parameters
                if validator_class == RecommendationFormValidator:
                    validated_params = validator_class.validate_recommendation_criteria(params)
                    boolean_params = validator_class.validate_boolean_params(params)
                    validated_params.update(boolean_params)
                else:
                    raise ValueError(f"Unknown validator class: {validator_class}")

                # Add validated params to kwargs
                kwargs["validated_params"] = validated_params

                return func(*args, **kwargs)

            except ValidationError as e:
                return error_response(f"Validation error: {e.message}", 400)
            except Exception as e:
                return error_response(f"Parameter validation failed: {str(e)}", 400)

        return wrapper

    return decorator
