"""Filtering logic for activity recommendations."""

from __future__ import annotations

from app.core.constants import AGE_FILTER_TOLERANCE
from app.core.models import ActivityModel, SearchCriteria


def apply_hard_filters(activities: list[ActivityModel], criteria: SearchCriteria) -> list[ActivityModel]:
    """Apply all hard filters to activity models."""
    filtered: list[ActivityModel] = []

    for activity in activities:
        # Age filter
        if criteria.target_age is not None:
            age_min_ok = activity.age_min <= criteria.target_age + AGE_FILTER_TOLERANCE
            age_max_ok = activity.age_max >= criteria.target_age - AGE_FILTER_TOLERANCE
            if not (age_min_ok and age_max_ok):
                continue

        # Format filter
        if criteria.format:
            if activity.format not in criteria.format:
                continue

        # Duration filter
        if criteria.target_duration is not None:
            if activity.duration_min_minutes > criteria.target_duration:
                continue

        # Resources filter - activities with missing resources should be filtered out
        if criteria.available_resources:
            activity_resources = activity.resources_needed or []
            if activity_resources:  # Only filter if activity requires resources
                available_set = {r.value.lower() for r in criteria.available_resources}
                required_set = {r.value.lower() for r in activity_resources}
                if not required_set.issubset(available_set):
                    continue  # Filter out activities with missing resources

        # Bloom level filter
        if criteria.bloom_levels:
            if not any(activity.bloom_level.value.lower() == level.value.lower() for level in criteria.bloom_levels):
                continue

        # Topics filter
        if criteria.preferred_topics:
            activity_topics = activity.topics or []
            if not any(topic in activity_topics for topic in criteria.preferred_topics):
                continue

        filtered.append(activity)

    return filtered
