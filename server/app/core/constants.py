"""Constants for the recommendation engine."""

from __future__ import annotations

from dataclasses import dataclass

# Bloom's taxonomy order for scoring and progression
BLOOM_ORDER: list[str] = ["Remember", "Understand", "Apply", "Analyze", "Evaluate", "Create"]


# Scoring Categories
@dataclass
class ScoringCategory:
    """Represents a scoring category with its impact level and description."""

    name: str
    impact: int  # 1-5 impact level (1=lowest, 5=highest)
    description: str


# Define scoring categories with impact levels (1-5 scale)
SCORING_CATEGORIES = {
    # Individual activity categories (for single activities)
    "age_appropriateness": ScoringCategory(
        name="age_appropriateness",
        impact=4,
        description="How well the activity matches the target age range",
    ),
    "bloom_level_match": ScoringCategory(
        name="bloom_level_match",
        impact=5,
        description="How well the activity matches the target Bloom's taxonomy level",
    ),
    "topic_relevance": ScoringCategory(
        name="topic_relevance",
        impact=4,
        description="How well the activity covers the preferred computational thinking topics",
    ),
    "duration_fit": ScoringCategory(
        name="duration_fit",
        impact=3,
        description="How well the total duration (activities + breaks) matches the target duration",
    ),
    # Series-specific categories (for multi-activity sequences)
    "series_cohesion": ScoringCategory(
        name="series_cohesion",
        impact=3,
        description="How well activities in a series work together (topic overlap + Bloom progression)",
    ),
}

# Age scoring constants
AGE_MAX_DISTANCE = 5  # Maximum age distance for partial scoring

# Bloom level scoring constants
BLOOM_ADJACENT_LEVELS = 1  # Number of adjacent levels to consider

# Priority category multiplier (applies to all priority categories)
PRIORITY_CATEGORY_MULTIPLIER = 2.0  # 2x impact for priority categories

# Valid priority categories (excludes series_cohesion which is only used for scoring)
PRIORITY_CATEGORIES = ["age_appropriateness", "bloom_level_match", "topic_relevance", "duration_fit"]


# Lesson plan generation limits
LESSON_PLAN_TOP_ACTIVITIES_LIMIT = 20  # Limit for performance in lesson plan generation

# Break duration constants
BREAK_MIN_DURATION = 5  # Minimum break duration in minutes
BREAK_DURATION_INCREMENT = 5  # Round break durations to nearest 5 minutes

# Default prep/cleanup times
DEFAULT_PREP_TIME = 0  # Default preparation time in minutes
DEFAULT_CLEANUP_TIME = 0  # Default cleanup time in minutes

# Age filter constants
AGE_FILTER_TOLERANCE = 2  # Age tolerance for hard filters

# Pipeline limits
DEFAULT_MAX_ACTIVITY_COUNT = 2
DEFAULT_RECOMMENDATION_LIMIT = 10

# Score normalization
SCORE_MAX_VALUE = 100  # Maximum score value for normalization
SCORE_MIN_VALUE = 0  # Minimum score value for normalization
