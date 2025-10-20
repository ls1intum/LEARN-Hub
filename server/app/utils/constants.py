"""Shared constants for dropdown values and validation."""

from app.core.models import (
    ActivityFormat,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    EnergyLevel,
)

# Dropdown field values
FORMAT_OPTIONS = [f.value for f in ActivityFormat]
RESOURCE_OPTIONS = [r.value for r in ActivityResource]
TOPIC_OPTIONS = [t.value for t in ActivityTopic]
BLOOM_LEVEL_OPTIONS = [b.value for b in BloomLevel]
ENERGY_LEVEL_OPTIONS = [e.value for e in EnergyLevel]

# Validation ranges
AGE_RANGE = {"min": 6, "max": 15}
DURATION_RANGE = {"min": 1, "max": 300}
PREP_CLEANUP_RANGE = {"min": 0, "max": 60}

# Time increment validation
TIME_INCREMENT_MINUTES = 5

# Default values
DEFAULT_ENERGY_LEVEL = EnergyLevel.MEDIUM.value
DEFAULT_PREP_TIME = 5
DEFAULT_CLEANUP_TIME = 5
