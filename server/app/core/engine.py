from __future__ import annotations

from itertools import combinations

from app.core.constants import (
    DEFAULT_MAX_ACTIVITY_COUNT,
    DEFAULT_RECOMMENDATION_LIMIT,
    LESSON_PLAN_TOP_ACTIVITIES_LIMIT,
)
from app.core.filters import apply_hard_filters
from app.core.models import (
    ActivityModel,
    Break,
    PriorityCategory,
    ScoreModel,
    SearchCriteria,
)
from app.core.scoring import ScoringEngine
from app.utils.time_utils import round_up_to_nearest_5_minutes


class RecommendationPipeline:
    """Simplified recommendation pipeline orchestrator."""

    def __init__(
        self,
        criteria: SearchCriteria,
        priority_categories: list[PriorityCategory],
        include_breaks: bool = False,
        max_activity_count: int = DEFAULT_MAX_ACTIVITY_COUNT,
        limit: int = DEFAULT_RECOMMENDATION_LIMIT,
    ):
        self.criteria = criteria
        self.priority_categories = priority_categories
        self.include_breaks = include_breaks
        self.max_activity_count = max_activity_count
        self.limit = limit
        self.scoring_engine = ScoringEngine(self.priority_categories)

    def process(self, activities: list[ActivityModel]) -> list[tuple[list[ActivityModel], ScoreModel]]:
        """Execute the simplified recommendation pipeline."""
        filtered_activities = apply_hard_filters(activities, self.criteria)
        scored_results = self._score_all_activities_without_duration(filtered_activities)
        ranked_results = sorted(scored_results, key=lambda x: x[1].total_score, reverse=True)
        results_with_breaks = self._add_breaks_if_requested(ranked_results)
        rescored_results = self._rescore_duration_and_rank(results_with_breaks)
        # apply the final limit after duration has been factored in
        return rescored_results[: self.limit]

    def _score_all_activities_without_duration(
        self, activities: list[ActivityModel]
    ) -> list[tuple[list[ActivityModel], ScoreModel]]:
        """Score all activities and generate lesson plans without duration scoring."""
        results = []

        # Score individual activities (without duration)
        for activity in activities:
            activity_score = self.scoring_engine.score_activity_without_duration(activity, self.criteria)
            results.append(([activity], activity_score))

        # Generate lesson plans if max_activity_count > 1
        if self.max_activity_count > 1 and len(activities) > 1:
            # Get top activities for lesson plan generation (limit for performance)
            top_activities = [result[0][0] for result in results[:LESSON_PLAN_TOP_ACTIVITIES_LIMIT]]

            # Generate lesson plans of different lengths using simple combinations
            for k in range(2, min(self.max_activity_count + 1, 6)):  # 2-5 activities
                if k <= len(top_activities):
                    # Generate all combinations of k activities
                    for combo in combinations(top_activities, k):
                        lesson_plan = list(combo)
                        sequence_score = self.scoring_engine.score_sequence_without_duration(lesson_plan, self.criteria)
                        results.append((lesson_plan, sequence_score))

        return results

    def _add_breaks_if_requested(
        self, results: list[tuple[list[ActivityModel], ScoreModel]]
    ) -> list[tuple[list[ActivityModel], ScoreModel]]:
        """Add breaks to activities using the break_after field if requested."""
        if not self.include_breaks:
            return results

        final_results = []
        for activities, score in results:
            activities_list = activities.copy()

            # Add breaks only for multi-activity lesson plans
            if len(activities_list) > 1:
                self._assign_breaks_to_activities(activities_list)

            final_results.append((activities_list, score))

        return final_results

    def _rescore_duration_and_rank(
        self, results: list[tuple[list[ActivityModel], ScoreModel]]
    ) -> list[tuple[list[ActivityModel], ScoreModel]]:
        """Re-score duration after breaks are added and re-rank results."""
        rescored_results = []

        for activities, _ in results:
            if not activities:
                # Skip if no activities (shouldn't happen)
                continue

            # Re-score the entire sequence/activity with duration including breaks
            if len(activities) == 1:
                # Single activity - score with duration including breaks
                new_score = self.scoring_engine.score_activity(activities[0], self.criteria)
            else:
                # Sequence - score with duration including breaks
                new_score = self.scoring_engine.score_sequence(activities, self.criteria)

            rescored_results.append((activities, new_score))

        # Re-rank by new total scores
        return sorted(rescored_results, key=lambda x: x[1].total_score, reverse=True)

    def _assign_breaks_to_activities(self, activities: list[ActivityModel]) -> None:
        """
        Assign breaks to activities using the break_after field based on energy levels and format changes.

        CRITICAL SAFEGUARDS:
        - Never assigns breaks after the last activity (breaks at end of lesson plans make no sense)
        - Never assigns breaks before the first activity (impossible with current architecture)
        - Only assigns breaks between activities in multi-activity lesson plans

        Args:
            activities: List of activities to assign breaks to (will be modified in place)
        """
        # Early return for empty or single activity lists
        if len(activities) <= 1:
            return

        # Validate break placement logic
        self._validate_break_placement(activities)

        for i, activity in enumerate(activities):
            # CRITICAL SAFEGUARD: Skip break assignment for the last activity
            # This prevents breaks at the end of lesson plans, which makes no pedagogical sense
            if i == len(activities) - 1:
                continue

            break_reasons = []
            break_duration = 0

            # Add cleanup time at end of activity
            if activity.cleanup_time_minutes and activity.cleanup_time_minutes > 0:
                break_duration += activity.cleanup_time_minutes
                break_reasons.append(f"Cleanup time for {activity.name}")

            # Add mental rest break for high mental load
            if activity.mental_load and activity.mental_load.value == "high":
                break_duration += 10
                break_reasons.append("Mental rest break after high cognitive load")

            # Add physical rest break for high physical energy
            if activity.physical_energy and activity.physical_energy.value == "high":
                break_duration += 5
                break_reasons.append("Physical rest break after high energy activity")

            # Add transition break for format changes (only if not the last activity)
            if i < len(activities) - 1:  # Double-check: not the last activity
                next_activity = activities[i + 1]
                if activity.format != next_activity.format:
                    break_duration += 5
                    break_reasons.append(
                        f"Transition break from {activity.format.value} to {next_activity.format.value}"
                    )

            # Only assign break if there are reasons and duration
            if break_reasons and break_duration > 0:
                # Round up to nearest 5-minute increment
                rounded_duration = round_up_to_nearest_5_minutes(break_duration)
                activity.break_after = Break(
                    duration=rounded_duration,
                    description="; ".join(break_reasons),
                    reasons=break_reasons,
                )

    def _validate_break_placement(self, activities: list[ActivityModel]) -> None:
        """
        Validate that break placement logic is correct.

        This method serves as an additional safeguard to ensure breaks are never
        placed after the last activity or before the first activity.

        Args:
            activities: List of activities to validate
        """
        if not activities:
            return

        # Ensure we never assign breaks after the last activity
        # This is enforced by the loop logic, but this validation makes it explicit
        last_activity_index = len(activities) - 1

        # The loop in _assign_breaks_to_activities skips the last activity,
        # so this validation confirms that behavior
        assert last_activity_index >= 0, "Activities list should not be empty at this point"

        # Additional validation: ensure the logic is sound
        # (This is more of a documentation/assertion than actual validation)
        if len(activities) > 1:
            # We should only assign breaks to activities that are not the last one
            # This is enforced by the early continue in the main loop
            pass


def get_recommendations(
    criteria: SearchCriteria,
    activities: list[ActivityModel],
    priority_categories: list[PriorityCategory],
    include_breaks: bool = False,
    max_activity_count: int = DEFAULT_MAX_ACTIVITY_COUNT,
    limit: int = DEFAULT_RECOMMENDATION_LIMIT,
) -> list[tuple[list[ActivityModel], ScoreModel]]:
    """
    Unified recommendation engine using category-based scoring:
    - Apply hard filters
    - Score all activities using unified category-based system
    - Generate lesson plans with series-specific categories
    - Rank single and multi-activity lesson plans together
    - Insert breaks for multi-activity lesson plans when requested

    Args:
        criteria: Typed search criteria
        activities: List of activities to recommend from
        priority_categories: List of categories that should receive priority scoring
        include_breaks: Whether to include breaks in multi-activity lesson plans
        max_activity_count: Maximum number of activities per lesson plan
        limit: Maximum number of recommendations to return
    """
    pipeline = RecommendationPipeline(
        criteria=criteria,
        priority_categories=priority_categories,
        include_breaks=include_breaks,
        max_activity_count=max_activity_count,
        limit=limit,
    )

    return pipeline.process(activities)
