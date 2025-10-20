from dataclasses import dataclass
from enum import Enum
from typing import Literal

import pydantic


class ActivityFormat(str, Enum):
    UNPLUGGED = "unplugged"
    DIGITAL = "digital"
    HYBRID = "hybrid"


class ActivityResource(str, Enum):
    COMPUTERS = "computers"
    TABLETS = "tablets"
    HANDOUTS = "handouts"
    BLOCKS = "blocks"
    ELECTRONICS = "electronics"
    STATIONERY = "stationery"


class PriorityCategory(str, Enum):
    """Enum for categories that have priority multipliers - used by API to indicate high priority categories."""

    AGE_APPROPRIATENESS = "age_appropriateness"
    TOPIC_RELEVANCE = "topic_relevance"
    DURATION_FIT = "duration_fit"
    BLOOM_LEVEL_MATCH = "bloom_level_match"
    SERIES_DURATION_FIT = "series_duration_fit"


class BloomLevel(str, Enum):
    REMEMBER = "remember"
    UNDERSTAND = "understand"
    APPLY = "apply"
    ANALYZE = "analyze"
    EVALUATE = "evaluate"
    CREATE = "create"


class ActivityTopic(str, Enum):
    DECOMPOSITION = "decomposition"
    PATTERNS = "patterns"
    ABSTRACTION = "abstraction"
    ALGORITHMS = "algorithms"


class EnergyLevel(str, Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class Break(pydantic.BaseModel):
    """Represents a break between activities"""

    type: Literal["break"] = "break"
    duration: int
    description: str
    reasons: list[str]  # List of reasons why this break was necessary


class ActivityModel(pydantic.BaseModel):
    type: Literal["activity"] = "activity"
    id: int | None = None
    name: str
    description: str
    source: str | None = None
    age_min: int
    age_max: int
    format: ActivityFormat
    resources_needed: list[ActivityResource] = []
    bloom_level: BloomLevel
    duration_min_minutes: int
    duration_max_minutes: int | None = None
    topics: list[ActivityTopic] = []
    mental_load: EnergyLevel | None = None
    physical_energy: EnergyLevel | None = None
    prep_time_minutes: int | None = None
    cleanup_time_minutes: int | None = None
    pdf_path: str | None = None
    pdf_uploaded_at: str | None = None
    # Break that should happen after this activity (not stored in database)
    break_after: Break | None = None


@dataclass
class SearchCriteria:
    """Clean data structure for search criteria"""

    name: str | None = None
    target_age: int | None = None
    format: list[ActivityFormat] | None = None
    bloom_levels: list[BloomLevel] | None = None
    target_duration: int | None = None
    available_resources: list[ActivityResource] | None = None
    preferred_topics: list[ActivityTopic] | None = None


@dataclass
class CategoryScore:
    """Individual category score."""

    category: str
    score: int  # 0-100
    impact: int  # 1-5 impact level
    priority_multiplier: float = 1.0
    is_priority: bool = False


@dataclass
class ScoreModel:
    """Unified score model for activities and sequences with category breakdown."""

    total_score: int  # 0-100 weighted total (computed at the end)
    category_scores: dict[str, CategoryScore]  # category -> score details
    priority_categories: list[PriorityCategory]
    is_sequence: bool = False  # True for lesson plans, False for single activities
    activity_count: int = 1  # Number of activities in this score (1 for single, >1 for sequences)
