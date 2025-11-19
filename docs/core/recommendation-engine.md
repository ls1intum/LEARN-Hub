# Recommendation Engine

## Overview

The LEARN-Hub recommendation engine uses a category-based scoring system to match educational activities with teacher requirements. It quantifies pedagogical fit across age appropriateness, learning objectives, resources, and time constraints, generating recommendations for both individual activities and multi-activity lesson plans.

## Scoring Architecture

### Dimensions & Weighting

The engine evaluates activities across five independent categories. Each category generates a raw score [0-100], which is aggregated using a weighted average. Teachers can prioritize categories to apply a 2x multiplier.

| Category | Impact | Description |
|----------|--------|-------------|
| **Age Appropriateness** | 4 | Tolerance-based matching (±5 years) with linear decay. |
| **Bloom's Alignment** | 5 | Hierarchical matching: Exact=100, Adjacent=50, Others=0. |
| **Topic Relevance** | 4 | Proportional coverage of requested topics. |
| **Duration Fit** | 3 | Checks fit within constraints; includes 50% excess tolerance. |
| **Series Cohesion** | 3 | For lesson plans: Topic overlap (50%) + Bloom progression (50%). |

**Formula**: `total_score = Σ(category_score × impact × priority_multiplier) / Σ(impact × priority_multiplier)`

### Scoring Logic

*   **Age**: Uses a linear decay for ages outside the target range but within tolerance, acknowledging that activity difficulty is approximate.
*   **Bloom's**: Recognizes cognitive hierarchy (Remember → Create). Adjacent levels get partial credit as they represent scaffolded steps.
*   **Topics**: calculated as `(matching_topics / requested_topics) × 100`.
*   **Duration**: Penalizes activities exceeding target duration, but allows slight overage (up to 50%) to prevent rigid rejection of good content.

## Processing Pipeline

### 1. Hard Filtering
Before scoring, activities must pass hard filters to eliminate incompatible items:
*   **Age**: Within ±2 years of target.
*   **Format**: Matches preferred format (unplugged/digital/hybrid).
*   **Resources**: Requires only available resources.
*   **Bloom/Topics**: Must match at least one preferred level/topic.

### 2. Two-Stage Scoring
To optimize performance (<2s response), the engine uses a two-stage process:

**Stage 1: Preliminary Scoring & Ranking**
*   Score all filtered activities excluding duration (computationally cheap).
*   Rank results.
*   Select top **25** activities for lesson plan combination generation.

**Stage 2: Duration Re-scoring**
*   Generate combinations (if requested) from the top 25 candidates.
*   Insert breaks into lesson plans.
*   Re-score candidates including duration fit (computationally expensive).
*   Final ranking and selection.

### 3. Lesson Plan Generation
Lesson plans are generated via combinatorial analysis of the top **25** activities from Stage 1.
*   **Limit**: Top 25 activities (reduced from O(n^k) to O(25^k)).
*   **Size**: Configurable 2-5 activities per plan.

## Break System

The system automatically inserts breaks in multi-activity plans to account for transitions and fatigue. Breaks are added to the total duration.

*   **Cleanup**: Based on activity metadata.
*   **Mental Rest**: 10 mins after "high" mental load activities.
*   **Physical Rest**: 5 mins after "high" physical energy activities.
*   **Transition**: 5 mins when format changes (e.g., Unplugged → Digital).

*Note: Breaks are never added after the final activity.*

## System Integration

The engine resides in `app/core/` and is decoupled from the web framework.
*   **Input**: `SearchCriteria`, `List[ActivityModel]`, configuration.
*   **Output**: Recommendations with detailed score breakdowns.

### Performance Optimizations
1.  **Two-Stage Scoring**: Defers expensive duration/break logic.
2.  **Hard Filters**: Reduces the initial candidate pool.
3.  **Combinatorial Limit**: Restricts lesson plan generation to the top 25 activities.
4.  **Integer Math**: Uses [0-100] integer scales for efficiency.

## Trade-offs & Design Evaluation

| Aspect | Choice | Rationale |
|--------|--------|-----------|
| **Algorithm** | Category Scoring | No training data needed; highly explainable to teachers. |
| **Aggregation** | Weighted Average | Normalizes scores; handles trade-offs gracefully. |
| **Performance** | Two-Stage + Top-25 | Enables interactive response times for combinatorial problems. |
| **Breaks** | Automated | Reduces teacher cognitive load; standardizes transition times. |

**Limitations**:
*   **Manual Weights**: Impact weights are heuristically defined, not learned.
*   **Local Optima**: Limiting combinations to the top 25 individual activities might miss optimal plans composed of medium-scoring activities that fit well together.
*   **Static Rules**: Break durations and tolerances are fixed constants.
