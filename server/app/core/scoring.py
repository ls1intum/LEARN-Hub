"""Refactored scoring logic with explainable categories and maintainable constants."""

from __future__ import annotations

from app.core.constants import (
    AGE_MAX_DISTANCE,
    BLOOM_ADJACENT_LEVELS,
    BLOOM_ORDER,
    PRIORITY_CATEGORY_MULTIPLIER,
    SCORING_CATEGORIES,
)
from app.core.models import (
    ActivityModel,
    CategoryScore,
    PriorityCategory,
    ScoreModel,
    SearchCriteria,
)


class ScoringEngine:
    """Main scoring engine with explainable category-based scoring."""

    def __init__(self, priority_categories: list[PriorityCategory] | None = None):
        self.priority_categories = priority_categories or []

    def score_activity(self, activity: ActivityModel, criteria: SearchCriteria) -> ScoreModel:
        """Score a single activity with category breakdown."""
        category_scores = {}

        category_scores["age_appropriateness"] = self._score_age_appropriateness(activity, criteria)
        category_scores["bloom_level_match"] = self._score_bloom_level_match(activity, criteria)
        category_scores["topic_relevance"] = self._score_topic_relevance(activity, criteria)
        category_scores["duration_fit"] = self._score_duration_fit([activity], criteria)

        total_score = self._calculate_weighted_total(category_scores)

        return ScoreModel(
            total_score=total_score,
            category_scores=category_scores,
            priority_categories=self.priority_categories,
            is_sequence=False,
            activity_count=1,
        )

    def score_activity_without_duration(self, activity: ActivityModel, criteria: SearchCriteria) -> ScoreModel:
        """Score a single activity without duration scoring."""
        category_scores = {}

        category_scores["age_appropriateness"] = self._score_age_appropriateness(activity, criteria)
        category_scores["bloom_level_match"] = self._score_bloom_level_match(activity, criteria)
        category_scores["topic_relevance"] = self._score_topic_relevance(activity, criteria)

        total_score = self._calculate_weighted_total(category_scores)

        return ScoreModel(
            total_score=total_score,
            category_scores=category_scores,
            priority_categories=self.priority_categories,
            is_sequence=False,
            activity_count=1,
        )

    def score_sequence(self, activities: list[ActivityModel], criteria: SearchCriteria) -> ScoreModel:
        """Score a sequence of activities (lesson plan) using unified category-based scoring."""
        category_scores = {}

        activity_scores = []
        for activity in activities:
            activity_scores.append(self.score_activity(activity, criteria))

        individual_category_scores = self._calculate_average_individual_scores(activity_scores)

        for category_name, avg_score in individual_category_scores.items():
            category_scores[category_name] = avg_score

        category_scores["series_cohesion"] = self._score_series_cohesion_category(activities)
        category_scores["duration_fit"] = self._score_duration_fit(activities, criteria)

        total_score = self._calculate_weighted_total(category_scores)

        return ScoreModel(
            total_score=total_score,
            category_scores=category_scores,
            priority_categories=self.priority_categories,
            is_sequence=True,
            activity_count=len(activities),
        )

    def score_sequence_without_duration(self, activities: list[ActivityModel], criteria: SearchCriteria) -> ScoreModel:
        """Score a sequence of activities without duration scoring."""
        category_scores = {}

        activity_scores = []
        for activity in activities:
            activity_scores.append(self.score_activity_without_duration(activity, criteria))

        individual_category_scores = self._calculate_average_individual_scores(activity_scores)

        for category_name, avg_score in individual_category_scores.items():
            category_scores[category_name] = avg_score

        category_scores["series_cohesion"] = self._score_series_cohesion_category(activities)

        total_score = self._calculate_weighted_total(category_scores)

        return ScoreModel(
            total_score=total_score,
            category_scores=category_scores,
            priority_categories=self.priority_categories,
            is_sequence=True,
            activity_count=len(activities),
        )

    def _score_age_appropriateness(self, activity: ActivityModel, criteria: SearchCriteria) -> CategoryScore:
        """Score how well the activity matches the target age range."""
        category = SCORING_CATEGORIES["age_appropriateness"]
        target_age = criteria.target_age

        if target_age is None or activity.age_min is None or activity.age_max is None:
            return CategoryScore(
                category=category.name,
                score=0,
                impact=category.impact,
            )

        raw_score = 0.0
        if activity.age_min <= target_age <= activity.age_max:
            raw_score = 100.0
        else:
            distance = activity.age_min - target_age if target_age < activity.age_min else target_age - activity.age_max
            if distance <= AGE_MAX_DISTANCE:
                raw_score = ((AGE_MAX_DISTANCE - distance) / AGE_MAX_DISTANCE) * 100.0
            else:
                raw_score = 0.0

        priority_multiplier = 1.0
        is_priority = False
        if PriorityCategory.AGE_APPROPRIATENESS in self.priority_categories:
            priority_multiplier = PRIORITY_CATEGORY_MULTIPLIER
            is_priority = True

        final_score = min(max(int(raw_score), 0), 100)

        return CategoryScore(
            category=category.name,
            score=final_score,
            impact=category.impact,
            priority_multiplier=priority_multiplier,
            is_priority=is_priority,
        )

    def _score_bloom_level_match(self, activity: ActivityModel, criteria: SearchCriteria) -> CategoryScore:
        """Score how well the activity matches the target Bloom's taxonomy level."""
        category = SCORING_CATEGORIES["bloom_level_match"]
        target_bloom_levels = criteria.bloom_levels

        if not target_bloom_levels or not activity.bloom_level:
            return CategoryScore(
                category=category.name,
                score=0,
                impact=category.impact,
            )

        activity_bloom = activity.bloom_level.value.lower()
        best_raw_score = 0.0

        for target_bloom in target_bloom_levels:
            target_bloom_lower = target_bloom.value.lower()

            if target_bloom_lower == activity_bloom:
                raw_score = 100.0
                if raw_score > best_raw_score:
                    best_raw_score = raw_score
            elif target_bloom.value in BLOOM_ORDER and activity.bloom_level.value in BLOOM_ORDER:
                try:
                    target_idx = BLOOM_ORDER.index(target_bloom.value.capitalize())
                    activity_idx = BLOOM_ORDER.index(activity.bloom_level.value.capitalize())
                    distance = abs(target_idx - activity_idx)

                    if distance == BLOOM_ADJACENT_LEVELS:
                        raw_score = 50.0
                        if raw_score > best_raw_score:
                            best_raw_score = raw_score
                except ValueError:
                    pass

        priority_multiplier = 1.0
        is_priority = False
        if PriorityCategory.BLOOM_LEVEL_MATCH in self.priority_categories:
            priority_multiplier = PRIORITY_CATEGORY_MULTIPLIER
            is_priority = True

        final_score = min(max(int(best_raw_score), 0), 100)

        return CategoryScore(
            category=category.name,
            score=final_score,
            impact=category.impact,
            priority_multiplier=priority_multiplier,
            is_priority=is_priority,
        )

    def _score_topic_relevance(self, activity: ActivityModel, criteria: SearchCriteria) -> CategoryScore:
        """Score how well the activity covers the preferred computational thinking topics."""
        category = SCORING_CATEGORIES["topic_relevance"]
        preferred_topics = criteria.preferred_topics

        if not preferred_topics:
            return CategoryScore(category=category.name, score=0, impact=category.impact)

        activity_topics = activity.topics or []
        if not activity_topics:
            return CategoryScore(category=category.name, score=0, impact=category.impact)

        preferred_set = {t.value.lower() for t in preferred_topics}
        activity_set = {t.value.lower() for t in activity_topics}
        matches = len(preferred_set & activity_set)
        total_preferred = len(preferred_set)

        if total_preferred == 0:
            raw_score = 0.0
        else:
            raw_score = (matches / total_preferred) * 100.0

        priority_multiplier = 1.0
        is_priority = False
        if PriorityCategory.TOPIC_RELEVANCE in self.priority_categories:
            priority_multiplier = PRIORITY_CATEGORY_MULTIPLIER
            is_priority = True

        final_score = min(max(int(raw_score), 0), 100)

        return CategoryScore(
            category=category.name,
            score=final_score,
            impact=category.impact,
            priority_multiplier=priority_multiplier,
            is_priority=is_priority,
        )

    def _score_duration_fit(self, activities: list[ActivityModel], criteria: SearchCriteria) -> CategoryScore:
        """Score how well the total duration (activities + breaks) matches the target duration."""
        category = SCORING_CATEGORIES["duration_fit"]
        target_duration = criteria.target_duration

        if target_duration is None:
            return CategoryScore(
                category=category.name,
                score=0,
                impact=category.impact,
            )

        total_duration = 0
        for idx, activity in enumerate(activities):
            if activity.duration_min_minutes is not None:
                duration_max = activity.duration_max_minutes or activity.duration_min_minutes
                total_duration += (activity.duration_min_minutes + duration_max) // 2

            # Exclude any break_after on the last activity from duration scoring
            is_last = idx == len(activities) - 1
            if not is_last and activity.break_after:
                total_duration += activity.break_after.duration

        if total_duration == 0:
            return CategoryScore(
                category=category.name,
                score=0,
                impact=category.impact,
            )

        raw_score = 0.0

        if total_duration == target_duration:
            raw_score = 100.0
        elif total_duration < target_duration:
            # Penalize being too short
            shortfall_ratio = (target_duration - total_duration) / target_duration
            if shortfall_ratio <= 0.5:
                # Linear decay from 100% to 50% as it gets shorter (down to half duration)
                raw_score = (1 - shortfall_ratio) * 100.0
            else:
                # Too short (less than half the target) gets 0
                raw_score = 0.0
        else:
            # Penalize being too long (existing logic)
            excess_ratio = (total_duration - target_duration) / target_duration
            if excess_ratio <= 0.5:
                raw_score = (1 - excess_ratio) * 100.0
            else:
                raw_score = 0.0

        priority_multiplier = 1.0
        is_priority = False
        if PriorityCategory.DURATION_FIT in self.priority_categories:
            priority_multiplier = PRIORITY_CATEGORY_MULTIPLIER
            is_priority = True

        final_score = min(max(int(raw_score), 0), 100)

        return CategoryScore(
            category=category.name,
            score=final_score,
            impact=category.impact,
            priority_multiplier=priority_multiplier,
            is_priority=is_priority,
        )

    def _calculate_average_individual_scores(self, activity_scores: list[ScoreModel]) -> dict[str, CategoryScore]:
        """Calculate average scores for each individual category across activities."""
        if not activity_scores:
            return {}

        # Get all category names from the first activity
        category_names = list(activity_scores[0].category_scores.keys())
        avg_scores = {}

        for category_name in category_names:
            # Calculate average score for this category across all activities
            total_score = sum(score.category_scores[category_name].score for score in activity_scores)
            avg_score = total_score // len(activity_scores)

            # Get the category definition
            category = SCORING_CATEGORIES[category_name]

            # Create average CategoryScore with priority information
            priority_multiplier = 1.0
            is_priority = False
            if category_name.lower() in [cat.value.lower() for cat in self.priority_categories]:
                priority_multiplier = PRIORITY_CATEGORY_MULTIPLIER
                is_priority = True

            avg_scores[category_name] = CategoryScore(
                category=category.name,
                score=avg_score,
                impact=category.impact,
                priority_multiplier=priority_multiplier,
                is_priority=is_priority,
            )

        return avg_scores

    def _score_series_cohesion_category(self, activities: list[ActivityModel]) -> CategoryScore:
        """Score how well activities in a series work together as a CategoryScore.

        Series cohesion is always applied for series recommendations regardless of priority settings.
        """
        category = SCORING_CATEGORIES["series_cohesion"]

        if len(activities) == 1:
            # Single activity gets perfect cohesion score
            return CategoryScore(
                category=category.name,
                score=int(category.impact * 20),  # Convert impact to score (3 * 20 = 60)
                impact=category.impact,
                priority_multiplier=1.0,  # Always 1.0 for series cohesion
                is_priority=False,  # Never a priority category
            )

        # For multiple activities, score based on topic overlap and Bloom progression
        cohesion_score = 0

        # Check topic overlap between consecutive activities
        topic_overlap_score = 0
        for i in range(len(activities) - 1):
            current_topics = {t.value.lower() for t in (activities[i].topics or [])}
            next_topics = {t.value.lower() for t in (activities[i + 1].topics or [])}
            if current_topics and next_topics:
                overlap = len(current_topics & next_topics) / len(current_topics | next_topics)
                topic_overlap_score += overlap

        if len(activities) > 1:
            topic_overlap_score = (topic_overlap_score / (len(activities) - 1)) * 50  # Max 50 points

        # Check Bloom progression (non-decreasing)
        bloom_progression_score = 0
        if len(activities) > 1:
            bloom_indices = []
            for activity in activities:
                try:
                    idx = BLOOM_ORDER.index(activity.bloom_level.value.capitalize())
                    bloom_indices.append(idx)
                except ValueError:
                    bloom_indices.append(0)

            # Check if progression is non-decreasing
            is_progressive = all(bloom_indices[i] <= bloom_indices[i + 1] for i in range(len(bloom_indices) - 1))
            bloom_progression_score = 50 if is_progressive else 25

        cohesion_score = int(topic_overlap_score + bloom_progression_score)
        cohesion_score = min(cohesion_score, 100)  # Cap at 100

        # Ensure score is 0-100
        score = min(max(cohesion_score, 0), 100)

        return CategoryScore(
            category=category.name,
            score=score,
            impact=category.impact,
            priority_multiplier=1.0,  # Always 1.0 for series cohesion
            is_priority=False,  # Never a priority category
        )

    def _calculate_weighted_total(self, category_scores: dict[str, CategoryScore]) -> int:
        """Calculate the weighted total score from category scores - simplified to 0-100 range."""
        if not category_scores:
            return 0

        # Calculate weighted average of category scores
        weighted_sum = 0.0
        total_weight = 0.0

        for category_score in category_scores.values():
            # Use impact as weight, score is already 0-100
            weight = category_score.impact
            weighted_sum += category_score.score * weight
            total_weight += weight

        if total_weight == 0:
            return 0

        # Calculate weighted average and ensure it's 0-100
        weighted_average = weighted_sum / total_weight
        return min(max(int(weighted_average), 0), 100)
