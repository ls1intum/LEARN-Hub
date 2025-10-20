"""Metadata API endpoints with Flask-OpenAPI3."""

from __future__ import annotations

from flask_openapi3 import Tag

from app.core.constants import PRIORITY_CATEGORIES
from app.core.models import (
    ActivityFormat,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    EnergyLevel,
)
from app.utils.pydantic_models import ErrorResponse, FieldValuesResponse
from app.utils.response_helpers import error_response, success_response

# Define API tag for OpenAPI
meta_tag = Tag(name="meta", description="Metadata and field values")


def register_meta_routes(api):
    """Register metadata routes with Flask-OpenAPI3."""

    @api.get(
        "/api/meta/field-values",
        tags=[meta_tag],
        responses={200: FieldValuesResponse, 500: ErrorResponse},
        summary="Get field values",
        description="Get field values for enums used by client",
    )
    def get_field_values():
        """Get field values."""
        try:
            data = {
                "format": [e.value for e in ActivityFormat],
                "resources_available": [e.value for e in ActivityResource],
                "bloom_level": [e.value for e in BloomLevel],
                "topics": [e.value for e in ActivityTopic],
                "mental_load": [e.value for e in EnergyLevel],
                "physical_energy": [e.value for e in EnergyLevel],
                "priority_categories": PRIORITY_CATEGORIES,
            }
            return success_response(data)
        except Exception as e:
            return error_response(f"Failed to get field values: {str(e)}", 500)
