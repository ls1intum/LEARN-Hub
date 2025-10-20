# Recommendation System Logic

## Overview

The recommendation system generates educational activity recommendations using a unified category-based scoring approach with priority multipliers and automated activity sequencing.

## Core Concepts

### Activity Types
- **Single Activities**: Individual educational activities scored independently
- **Lesson Plans**: Sequences of 2-5 activities with pedagogical progression and series-specific scoring

### Scoring Categories

The system uses five main scoring categories, each with configurable impact levels (1-5 scale):

1. **Age Appropriateness** (Impact: 4)
   - Perfect match: 100 points if target age falls within activity's age range
   - Partial match: Weighted score based on distance from age range (±5 years tolerance)
   - Priority multiplier: 2x when age is marked as priority category

2. **Bloom's Taxonomy Alignment** (Impact: 5)
   - Exact match: 100 points for exact Bloom level match
   - Adjacent match: 50 points for adjacent Bloom levels (1 level away)
   - Priority multiplier: 2x when Bloom level is marked as priority category

3. **Topic Relevance** (Impact: 4)
   - Score based on percentage of matching topics between criteria and activity
   - Formula: (matching_topics / total_preferred_topics) × 100
   - Priority multiplier: 2x when topics are marked as priority category

4. **Duration Fit** (Impact: 3)
   - Perfect fit: 100 points if total duration ≤ target duration
   - Partial fit: Weighted score based on excess ratio (within 50% tolerance)
   - Priority multiplier: 2x when duration is marked as priority category

5. **Series Cohesion** (Impact: 3, sequences only)
   - Topic overlap: 50 points based on topic overlap between consecutive activities
   - Bloom progression: 50 points for non-decreasing Bloom level progression
   - Single activities: Perfect score (100 points)

## Recommendation Generation Process

### 1. Hard Filtering
- **Age Filter**: Activities outside age range ±2 years tolerance are excluded
- **Format Filter**: Activities not matching preferred format(s) are excluded
- **Duration Filter**: Activities exceeding target duration are excluded
- **Resource Filter**: Activities requiring unavailable resources are excluded
- **Bloom Level Filter**: Activities not matching preferred Bloom levels are excluded
- **Topic Filter**: Activities not covering any preferred topics are excluded

### 2. Scoring Pipeline
The system uses a two-stage scoring approach:

**Stage 1: Non-Duration Scoring**
- Score all activities and lesson plans without duration considerations
- Generate lesson plans using combinations of top 20 activities
- Rank results by total score for preliminary ordering

**Stage 2: Duration Re-scoring**
- Add breaks to multi-activity lesson plans if requested
- Re-score all results including duration fit
- Final ranking based on complete scores

### 3. Lesson Plan Generation
- **Combination-based approach**: Generate all combinations of 2-5 activities
- **Performance limit**: Use top 20 activities to reduce complexity from O(n^k) to O(20^k)
- **Length limits**: Configurable maximum activity count (default: 2, max: 5)
- **Scoring**: Each combination scored using unified category system

### 4. Break Calculation
Breaks are automatically calculated for multi-activity lesson plans:

- **Preparation time**: Based on activity-specific prep_time_minutes
- **Cleanup time**: Based on activity-specific cleanup_time_minutes
- **Mental rest**: 10-minute break after high mental load activities
- **Physical rest**: 5-minute break after high physical energy activities
- **Format transitions**: 5-minute break when switching between formats
- **Positioning**: Breaks positioned between activities with fractional positions

### 5. Final Ranking
- All results (single activities and lesson plans) ranked together
- Priority categories receive 2x multiplier during scoring
- Results limited to configured maximum (default: 10)
- Return format: `list[tuple[list[ActivityModel | Break], ScoreModel]]`

## Priority Categories System

Users can mark specific categories as high priority, which applies a 2x multiplier:

- `AGE_APPROPRIATENESS`: Age appropriateness scoring
- `TOPIC_RELEVANCE`: Topic relevance scoring  
- `DURATION_FIT`: Duration fit scoring
- `BLOOM_LEVEL_MATCH`: Bloom's taxonomy alignment scoring
- `SERIES_DURATION_FIT`: Series duration fit scoring (lesson plans only)

## Score Calculation

### Individual Activities
```python
total_score = Σ(category_score × impact) / Σ(impact)
```

### Lesson Plans
```python
# Average individual scores
avg_individual = Σ(individual_category_scores) / activity_count

# Add series-specific categories
total_score = Σ(avg_individual + series_cohesion + duration_fit) / Σ(impact)
```

### Category Scoring
Each category returns a `CategoryScore` with:
- `score`: 0-100 integer score
- `impact`: 1-5 impact level (weight)
- `priority_multiplier`: 1.0 or 2.0 for priority categories
- `is_priority`: Boolean indicating if this category has priority multiplier applied

## Response Structure

The system returns structured results containing:

- **Activities**: List of recommended activities and breaks
- **Score Model**: Detailed scoring breakdown with category scores
- **Priority Categories**: List of categories marked as high priority
- **Sequence Info**: Whether result is single activity or lesson plan
- **Activity Count**: Number of activities in the recommendation

## Configuration

### Scoring Categories
```python
SCORING_CATEGORIES = {
    "age_appropriateness": ScoringCategory(impact=4),
    "bloom_level_match": ScoringCategory(impact=5),
    "topic_relevance": ScoringCategory(impact=4),
    "duration_fit": ScoringCategory(impact=3),
    "series_cohesion": ScoringCategory(impact=3)
}
```

### System Limits
- Default max activity count: 2
- Maximum activity count: 5
- Default recommendation limit: 10
- Lesson plan generation limit: 20 activities
- Age filter tolerance: ±2 years
- Duration excess tolerance: 50%

### Break Rules
- Mental rest: 10 minutes after high mental load
- Physical rest: 5 minutes after high physical energy
- Format transition: 5 minutes between format changes
- Cleanup: Based on activity-specific cleanup_time_minutes

## System Features

### Two-Stage Scoring Pipeline
The system uses a two-stage scoring approach for improved performance:

1. **Stage 1 - Non-Duration Scoring**: Score all activities and lesson plans without duration considerations to quickly identify top candidates
2. **Stage 2 - Duration Re-scoring**: Re-score the top results including duration fit after breaks are added

This approach reduces computational overhead by avoiding expensive duration calculations for low-scoring activities.

### Break Assignment System
Breaks are assigned directly to activities using the `break_after` field:

- **Break Assignment**: Breaks are calculated and assigned to activities during the pipeline
- **Break Reasons**: Each break includes detailed reasons for why it is necessary
- **Position Tracking**: Breaks use fractional positioning (e.g., 0.5 for between activities)
- **Duration Calculation**: Break durations are included in total duration calculations for scoring

### Scoring Methods
The scoring engine includes specialized methods for different scenarios:

- `score_activity_without_duration()`: Fast scoring without duration considerations
- `score_sequence_without_duration()`: Fast sequence scoring without duration
- `_score_duration_fit_with_breaks()`: Duration scoring that includes break durations
- `_score_series_cohesion_category()`: Series cohesion scoring with topic overlap and Bloom progression