# Recommendation System Implementation

## Architecture Overview

The recommendation system is implemented as a Python package with a unified scoring engine:

```
server/app/core/
├── __init__.py          # Public API and exports
├── models.py            # Data models and enums
├── engine.py            # RecommendationPipeline orchestrator
├── scoring.py           # Unified ScoringEngine
├── filters.py           # Hard filtering logic
└── constants.py         # Configuration constants
```

## Core Components

### Data Models (`models.py`)

**Enums:**
- `ActivityFormat`: unplugged, digital, hybrid
- `ActivityResource`: computers, tablets, handouts, blocks, electronics, stationery
- `BloomLevel`: remember, understand, apply, analyze, evaluate, create
- `ActivityTopic`: decomposition, patterns, abstraction, algorithms
- `EnergyLevel`: low, medium, high
- `PriorityCategory`: age_appropriateness, topic_relevance, duration_fit, bloom_level_match, series_duration_fit

**Data Classes:**
- `SearchCriteria`: Clean search criteria structure
- `CategoryScore`: Individual category score with impact, priority multiplier, and priority flag
- `ScoreModel`: Unified score model with category breakdown
- `ActivityModel`: Pydantic model for validation and data transfer
- `Break`: Break information with position, duration, and reasons

### Unified Scoring Engine (`scoring.py`)

**Main Class:**
```python
class ScoringEngine:
    def __init__(self, priority_categories: list[PriorityCategory] = None)
    def score_activity(self, activity: ActivityModel, criteria: SearchCriteria) -> ScoreModel
    def score_sequence(self, activities: list[ActivityModel], criteria: SearchCriteria) -> ScoreModel
```

**Key Features:**
- **Unified scoring**: Same engine handles single activities and sequences
- **Category-based**: Each scoring dimension is a separate category with configurable impact
- **Priority support**: Priority categories receive 2x multiplier
- **Explainable**: Detailed category breakdown in ScoreModel
- **Client Integration**: Priority categories are selectable in the RecommendationForm UI
- **Flexible**: Can score with or without duration considerations
- **Two-stage scoring**: Non-duration scoring for performance, then duration re-scoring

**Scoring Methods:**
- `_score_age_appropriateness()`: Age range matching with distance tolerance
- `_score_bloom_level_match()`: Bloom taxonomy alignment with adjacent level support
- `_score_topic_relevance()`: Topic coverage percentage calculation
- `_score_duration_fit()`: Duration matching with excess tolerance
- Duration scoring includes break durations via `_score_duration_fit()` using `break_after` fields; breaks after the final activity are ignored for both scoring and API responses.
- `_score_series_cohesion_category()`: Topic overlap and Bloom progression for sequences

### Recommendation Pipeline (`engine.py`)

**Main Class:**
```python
class RecommendationPipeline:
    def __init__(self, criteria, priority_categories, include_breaks, max_activity_count, limit)
    def process(self, activities: list[ActivityModel]) -> list[tuple[list[ActivityModel], ScoreModel]]
```

**Processing Stages:**
1. **Hard Filtering**: Apply all filters using `apply_hard_filters()`
2. **Non-Duration Scoring**: Score all activities and lesson plans without duration
3. **Preliminary Ranking**: Sort by total score and limit results
4. **Break Addition**: Add breaks to multi-activity lesson plans if requested
5. **Duration Re-scoring**: Re-score with duration and re-rank results

**Lesson Plan Generation:**
- Uses `itertools.combinations()` for generating activity combinations
- Limited to top 20 activities for performance (O(20^k) instead of O(n^k))
- Generates sequences of length 2 to max_activity_count
- Each combination scored using unified ScoringEngine

### Hard Filtering (`filters.py`)

**Main Function:**
```python
def apply_hard_filters(activities: list[ActivityModel], criteria: SearchCriteria) -> list[ActivityModel]
```

**Filter Types:**
- **Age Filter**: ±2 years tolerance around target age
- **Format Filter**: Must match preferred format(s)
- **Duration Filter**: Must not exceed target duration
- **Resource Filter**: Must not require unavailable resources
- **Bloom Level Filter**: Must match preferred Bloom level(s)
- **Topic Filter**: Must cover at least one preferred topic

### Configuration (`constants.py`)

**Scoring Categories:**
```python
SCORING_CATEGORIES = {
    "age_appropriateness": ScoringCategory(impact=4),
    "bloom_level_match": ScoringCategory(impact=5),
    "topic_relevance": ScoringCategory(impact=4),
    "duration_fit": ScoringCategory(impact=3),
    "series_cohesion": ScoringCategory(impact=3)
}
```

**System Constants:**
- `AGE_MAX_DISTANCE`: 5 years for partial age scoring
- `BLOOM_ADJACENT_LEVELS`: 1 level for adjacent Bloom matching
- `PRIORITY_CATEGORY_MULTIPLIER`: 2.0x for priority categories
- `DEFAULT_MAX_ACTIVITY_COUNT`: 2 activities per lesson plan
- `DEFAULT_RECOMMENDATION_LIMIT`: 10 recommendations
- `AGE_FILTER_TOLERANCE`: ±2 years for hard filtering

## Key Algorithms

### Category-Based Scoring

**Score Calculation:**
```python
def _calculate_weighted_total(self, category_scores: dict[str, CategoryScore]) -> int:
    weighted_sum = sum(score.score * score.impact for score in category_scores.values())
    total_weight = sum(score.impact for score in category_scores.values())
    return int(weighted_sum / total_weight) if total_weight > 0 else 0
```

**Priority Multiplier Application:**
```python
if PriorityCategory.AGE_APPROPRIATENESS in self.priority_categories:
    priority_multiplier = PRIORITY_CATEGORY_MULTIPLIER
    raw_score *= priority_multiplier
```

### Break Calculation

**Break Types:**
1. **Cleanup**: Based on `cleanup_time_minutes` field
2. **Mental Rest**: 10 minutes after high mental load
3. **Physical Rest**: 5 minutes after high physical energy
4. **Format Transition**: 5 minutes between format changes

**Break Assignment Logic:**
```python
# Cleanup time at end
if activity.cleanup_time_minutes and activity.cleanup_time_minutes > 0:
    break_duration += activity.cleanup_time_minutes
    break_reasons.append(f"Cleanup time for {activity.name}")

# Mental rest break for high mental load
if activity.mental_load and activity.mental_load.value == "high":
    break_duration += 10
    break_reasons.append("Mental rest break after high cognitive load")

# Physical rest break for high physical energy
if activity.physical_energy and activity.physical_energy.value == "high":
    break_duration += 5
    break_reasons.append("Physical rest break after high energy activity")

# Format transition breaks
if i < len(activities) - 1:  # Not the last activity
    next_activity = activities[i + 1]
    if activity.format != next_activity.format:
        break_duration += 5
        break_reasons.append(f"Transition break from {activity.format.value} to {next_activity.format.value}")
```

### Two-Stage Scoring Pipeline

**Stage 1: Non-Duration Scoring**
```python
def _score_all_activities_without_duration(self, activities):
    # Score individual activities without duration
    for activity in activities:
        score = self.scoring_engine.score_activity_without_duration(activity, criteria)
        results.append(([activity], score))
    
    # Generate lesson plans without duration
    for combo in combinations(top_activities, k):
        score = self.scoring_engine.score_sequence_without_duration(combo, criteria)
        results.append((combo, score))
```

**Stage 2: Duration Re-scoring**
```python
def _rescore_duration_and_rank(self, results):
    for activities, _ in results:
        if len(activities) == 1:
            new_score = self.scoring_engine.score_activity(activities[0], criteria)
        else:
            new_score = self.scoring_engine.score_sequence(activities, criteria)
        rescored_results.append((activities, new_score))
```

## Data Flow

1. **Input**: SearchCriteria + list[ActivityModel] + priority_categories
2. **Hard Filtering**: Apply filters → filtered activities
3. **Non-Duration Scoring**: Score all activities and combinations → preliminary results
4. **Preliminary Ranking**: Sort by score → limited results
5. **Break Addition**: Add breaks to multi-activity plans → results with breaks
6. **Duration Re-scoring**: Re-score with duration → final results
7. **Final Ranking**: Sort by complete score → ranked recommendations

## Performance Optimizations

### Two-Stage Scoring
- **Benefit**: Avoids expensive duration calculations for low-scoring activities
- **Implementation**: Score without duration first, then re-score top results with duration

### Activity Limiting
- **Lesson Plan Generation**: Limited to top 20 activities (O(20^k) vs O(n^k))
- **Combination Limits**: Maximum 5 activities per lesson plan
- **Result Limiting**: Configurable maximum recommendations (default: 10)

### Efficient Data Structures
- **Category Scores**: Dictionary-based category scoring for O(1) access
- **Priority Lookup**: Set-based priority category checking
- **Score Normalization**: Integer-based scoring (0-100) for performance

## API Integration

The core module integrates with the REST API through:

```python
# server/app/api/activities/recommendations.py
from app.core import get_recommendations

def get_activity_recommendations(criteria: dict) -> list[tuple[list[ActivityModel], ScoreModel]]:
    activities = load_activities_from_db(db)
    return get_recommendations(
        criteria=SearchCriteria(**criteria),
        activities=activities,
        priority_categories=priority_categories,
        include_breaks=include_breaks,
        max_activity_count=max_activity_count,
        limit=limit
    )
```

## Error Handling

- **Missing Data**: Graceful handling of None values in criteria and activities
- **Empty Results**: Returns empty list when no activities match filters
- **Invalid Combinations**: Skips invalid activity combinations during generation
- **Score Bounds**: Ensures all scores are within 0-100 range