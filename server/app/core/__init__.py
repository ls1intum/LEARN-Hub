"""Core recommendation engine module.

This module contains the core recommendation system components:
- models: Data models and enums for the recommendation system
- engine: Main recommendation engine logic
- scoring: Scoring algorithms for recommendations
- constants: Configuration constants for the recommendation system
"""

from . import engine
from .constants import BLOOM_ORDER

# Import the main function from engine
from .engine import get_recommendations
from .models import (
    ActivityFormat,
    ActivityModel,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    Break,
    EnergyLevel,
    PriorityCategory,
)

__all__ = [
    # Engine
    "engine",
    "get_recommendations",
    # Models
    "ActivityFormat",
    "ActivityResource",
    "ActivityTopic",
    "BloomLevel",
    "EnergyLevel",
    "PriorityCategory",
    "ActivityModel",
    "Break",
    # Constants
    "BLOOM_ORDER",
]
