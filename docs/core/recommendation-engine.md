# Recommendation Engine

## Overview

The LEARN-Hub recommendation engine implements a category-based scoring system designed to match educational activities with teacher requirements. The system addresses the challenge of selecting appropriate computer science education activities by quantifying pedagogical fit across multiple dimensions including age appropriateness, learning objectives, resource availability, and time constraints.

The engine generates recommendations for both individual activities and multi-activity lesson plans, supporting teachers in both single-activity selection and comprehensive lesson planning workflows.

## Design Philosophy

### Multi-Dimensional Scoring

The engine evaluates activities across five independent scoring dimensions (categories), each capturing a distinct aspect of pedagogical fit:

1. **Age Appropriateness**: Alignment between activity difficulty and student capabilities
2. **Bloom's Taxonomy Alignment**: Match between learning objectives and target cognitive levels
3. **Topic Relevance**: Coverage of desired computer science concepts
4. **Duration Fit**: Compatibility with available class time
5. **Series Cohesion**: Pedagogical progression in multi-activity sequences

This categorical approach provides explainability—teachers can understand *why* an activity was recommended by examining individual category scores. The transparency supports teacher agency; recommendations inform rather than dictate decisions.

### Priority Weighting System

Teachers can designate specific categories as high priority, applying a 2x multiplier to those scores. This mechanism acknowledges that pedagogical priorities vary by context:

- A teacher with mixed-age groups might prioritize **age appropriateness**
- A teacher covering specific curriculum requirements might prioritize **topic relevance**
- A teacher with strict time constraints might prioritize **duration fit**
- A teacher implementing scaffolded learning might prioritize **Bloom level alignment**

The priority system represents a design choice: provide algorithmic recommendations while preserving teacher autonomy and contextual expertise.

## Scoring Methodology

### Category Score Calculation

Each category produces a score in the range [0, 100] representing the degree of fit:

- **100**: Perfect match (e.g., target age falls within activity's age range)
- **50-99**: Partial match (e.g., adjacent Bloom level)
- **1-49**: Weak match (e.g., age significantly outside range but within tolerance)
- **0**: No match (e.g., incompatible format)

**Weighted Aggregation**: Category scores are combined using weighted averaging, where weights represent each category's relative importance:

```
total_score = Σ(category_score × impact × priority_multiplier) / Σ(impact × priority_multiplier)
```

- `impact`: Base weight for category (range 3-5)
- `priority_multiplier`: 2.0 for priority categories, 1.0 otherwise

This approach normalizes scores to [0, 100] regardless of the number of categories or priorities, maintaining score interpretability.

### Age Appropriateness Scoring

Age scoring implements a tolerance-based matching system:

**Perfect Match**: Target age falls within activity's [age_min, age_max] range → score = 100

**Partial Match**: Target age outside range but within tolerance (±5 years) → score weighted by distance from range boundary. A distance-based linear decay provides partial credit for near-matches while penalizing greater mismatches.

**Rationale**: Hard boundaries (matching only exact age ranges) would eliminate potentially suitable activities. The tolerance system acknowledges that:
- Activity difficulty ratings are approximate
- Teachers can adapt activities for different age groups
- Mixed-age classrooms benefit from flexible matching

### Bloom's Taxonomy Alignment

Bloom level scoring recognizes the hierarchical nature of cognitive skills:

**Exact Match**: Activity Bloom level equals target level → score = 100

**Adjacent Match**: Activity level is one step away in the Bloom hierarchy → score = 50

**No Match**: Activity level differs by 2+ steps → score = 0

**Hierarchy**: Remember → Understand → Apply → Analyze → Evaluate → Create

**Rationale**: Adjacent levels provide partial credit because:
- Lower levels are prerequisites for higher levels (students at "Apply" can handle "Understand")
- Higher levels implicitly include lower level practice
- Bloom levels are categorical, not continuous—more than one step away represents fundamentally different cognitive demands

This scoring reflects pedagogical theory where cognitive development follows a progression, making adjacent levels more interchangeable than distant levels.

### Topic Relevance Scoring

Topic scoring implements proportional coverage calculation:

```
score = (matching_topics / requested_topics) × 100
```

An activity covering 2 of 3 requested topics receives a score of 67, reflecting partial relevance.

**Rationale**: This approach provides partial credit for activities that address some but not all desired topics. Teachers can combine multiple partially-matching activities to achieve complete topic coverage, supporting flexible lesson planning.

**Alternative Considered**: Binary scoring (all topics or no match) was rejected because it would eliminate many useful activities and fail to distinguish between "covers 2/3 topics" and "covers 0/3 topics".

### Duration Fit Scoring

Duration scoring accommodates realistic classroom constraints:

**Perfect Fit**: Total duration ≤ target duration → score = 100

**Excess Tolerance**: Duration exceeds target but within 50% tolerance → score weighted by excess ratio

**Example**: 60-minute activity for 50-minute target → score = 100 - ((60-50)/50 × 50) = 90

**No Fit**: Duration exceeds target by >50% → score = 0

**Rationale**: The 50% excess tolerance acknowledges that:
- Teachers can abbreviate activities
- Time estimates are approximate
- Some overtime is acceptable for particularly relevant activities

The tolerance prevents rigid rejection of slightly-too-long activities while penalizing activities that significantly exceed available time.

### Series Cohesion Scoring

For multi-activity lesson plans, series cohesion evaluates pedagogical progression:

**Topic Overlap (50% weight)**: Measures thematic consistency between consecutive activities. Activities sharing topics create pedagogical continuity; completely unrelated activities reduce cohesion.

**Bloom Progression (50% weight)**: Evaluates cognitive progression. The system rewards non-decreasing Bloom levels (e.g., Remember → Apply → Analyze) reflecting scaffolded learning principles. Decreasing progressions (e.g., Evaluate → Remember) are penalized as pedagogically regressive.

**Rationale**: Multi-activity sequences should form coherent pedagogical units rather than disconnected exercises. Topic overlap ensures thematic consistency; Bloom progression ensures cognitive scaffolding. Equal weighting reflects that both aspects contribute equally to lesson quality.

**Single Activities**: Receive perfect cohesion scores (100) since cohesion is undefined for single items, preventing unfair penalty in mixed single/multi-activity result sets.

## Processing Pipeline

### Two-Stage Scoring Architecture

The engine implements a two-stage scoring process to optimize performance:

**Stage 1: Preliminary Scoring**
- Score all activities and combinations without duration calculations
- Rank results by preliminary scores
- Limit to top N candidates (configurable, default: sufficient to generate final results)

**Stage 2: Duration Re-scoring**
- Add breaks to multi-activity lesson plans (if requested)
- Re-score candidates including duration fit
- Final ranking based on complete scores

**Performance Rationale**: Duration scoring is computationally expensive because:
- Break calculation requires analyzing activity sequences
- Break duration impacts total duration
- Duration must be recalculated after break insertion

By deferring duration scoring until after preliminary ranking, the system avoids expensive calculations for low-scoring activities that won't appear in final results.

**Trade-off**: Two-stage scoring introduces complexity but enables interactive performance. The system is designed to generate recommendations within 2 seconds for acceptable user experience.

### Hard Filtering

Before scoring, activities pass through hard filters that eliminate incompatible items:

**Filter Criteria**:
- Age: Activities outside target age ±2 years tolerance
- Format: Activities not matching preferred format(s) (unplugged/digital/hybrid)
- Duration: Activities exceeding target duration (for single activities)
- Resources: Activities requiring unavailable resources
- Bloom Level: Activities not matching preferred level(s)
- Topics: Activities covering none of the preferred topics

**Rationale**: Hard filtering implements absolute requirements rather than preferences. An activity requiring computers cannot be used in a room without computers, regardless of its pedagogical quality. Hard filters improve performance (reducing activities to score) and result quality (eliminating impossible recommendations).

**Filter Tolerance**: Age and duration filters include tolerances (±2 years, slight excess allowed) recognizing that absolute boundaries are artificial. Teachers can adapt slightly mismatched activities, but significantly mismatched activities are unsuitable.

### Lesson Plan Generation

The system generates multi-activity lesson plans through combinatorial analysis:

**Combination Generation**: For a maximum of k activities per plan, the system generates all possible combinations of k activities from the available pool.

**Complexity Management**: To manage computational complexity (combinations grow as O(n^k)), the system:
- Limits combination generation to top 20 activities (not all activities)
- Configurable maximum activities per plan (default: 2, maximum: 5)
- Configurable result limit (default: 10 final recommendations)

**Rationale**: Generating combinations from all activities is computationally infeasible. Limiting to top 20 activities (by preliminary score) reduces computational complexity while maintaining the likelihood that high-quality lesson plans are composed of high-quality individual activities.

**Trade-off**: The combination limit means the system might miss optimal combinations composed of multiple medium-scoring activities. The performance gain (interactive response time) was prioritized over exhaustive combination search.

## Break Calculation System

### Automated Break Insertion

For multi-activity lesson plans, the system automatically calculates breaks between activities:

**Break Types**:

1. **Cleanup Breaks**: Based on activity-specific cleanup_time_minutes field
2. **Mental Rest Breaks**: 10 minutes after activities with high mental load
3. **Physical Rest Breaks**: 5 minutes after activities with high physical energy
4. **Format Transition Breaks**: 5 minutes when switching formats (e.g., unplugged → digital)

Multiple break reasons for a single transition are cumulative (e.g., cleanup + format transition = combined duration).

**Break Positioning**: Breaks are assigned to activities via a `break_after` field, embedded in the activity data structure. This approach keeps breaks contextually linked to the activities that necessitate them.

**Duration Impact**: Break durations are included in total duration calculations for lesson plans, ensuring duration scoring reflects actual class time requirements including transitions.

**Final Activity Handling**: Breaks after the final activity in a sequence are automatically removed—there's no need for transitions or cleanup after the lesson concludes.

**Design Rationale**: Automated break calculation addresses a planning challenge where teachers must account for transitions, cleanup, and student fatigue. The automated system:
- Ensures consistent break calculation
- Prevents overlooking necessary transitions
- Makes explicit the time requirements beyond activity duration

Break duration values (5, 10 minutes) represent reasonable estimates for transitions and rest periods.

### Break Fallback Handling

The API implements graceful degradation for break calculation failures:

If break calculation encounters errors (e.g., server processing issues), the system automatically retries the request without breaks rather than failing completely. This ensures users always receive recommendations, with breaks as an enhancement rather than a requirement.

This pattern reflects a general design principle: optional features should fail gracefully without preventing core functionality.

## Performance Optimizations

### Computational Efficiency

Several design decisions prioritize computational efficiency:

**Early Filtering**: Hard filters eliminate incompatible activities before scoring, reducing the scoring workload.

**Two-Stage Scoring**: Defers expensive duration calculations until after preliminary ranking.

**Combination Limits**: Limiting combinations to top-20 activities reduces complexity from O(n^k) to O(20^k), a massive reduction for large activity sets.

**Integer Scoring**: All scores are integers [0, 100] rather than floating-point, reducing memory usage and simplifying calculations.

**Efficient Data Structures**: Category scores stored in dictionaries provide O(1) access; priority categories stored in sets provide O(1) lookup.

**Target Performance**: The system is designed for interactive response times (<2 seconds). This constraint drove many optimization decisions.

### Scalability Considerations

The current implementation is optimized for the expected dataset size (~50 activities) and query patterns (interactive teacher requests). Scaling to larger datasets would require architectural changes:

- **Indexing**: Pre-compute activity characteristics for fast filtering
- **Caching**: Cache scoring results for common query patterns
- **Approximate Algorithms**: Use approximate matching instead of exhaustive combination generation
- **Parallel Processing**: Score activities in parallel across multiple cores

These optimizations were not implemented because they add complexity without benefit at current scale. The design prioritizes simplicity and maintainability over premature optimization.

## Algorithm Design Decisions

### Why Category-Based Scoring?

Alternative approaches such as collaborative filtering, content-based filtering, and rule-based systems were considered. These were rejected due to requirements for historical user data (unavailable), reduced explainability, or inflexibility in handling trade-offs.

Category-based scoring was chosen because it directly maps teacher requirements to scores, provides explainable recommendations through score breakdowns, handles partial matches gracefully, requires no training data, and adapts to individual requests via the priority system.

### Why Weighted Averaging?

Alternative aggregation methods (minimum score, additive, multiplicative) were considered and rejected due to issues with normalization, interpretability, or overly harsh handling of low scores in individual categories.

Weighted averaging was chosen because it produces normalized scores [0, 100] regardless of category count, supports varying category importance via impact weights, handles zero scores gracefully, and produces intuitive scores.

### Why Two-Stage Scoring?

Single-stage scoring was the initial implementation. Two-stage scoring separates duration calculations to optimize performance, as duration scoring is computationally expensive and unnecessary for activities that score low on non-duration criteria. The approach trades implementation complexity for performance while maintaining system maintainability.

## System Integration

### API Integration

The recommendation engine is packaged as an independent module (`app/core/`) that can be invoked from the API layer:

**Input**: SearchCriteria object + list of Activity objects + configuration parameters  
**Output**: List of recommendations, each containing activities and detailed score breakdown

This separation allows the algorithm to be tested independently of web framework concerns.

### Data Flow

1. **API receives request**: User submits search criteria via REST API
2. **Criteria validation**: Pydantic models validate and normalize input
3. **Activity retrieval**: Database query loads relevant activities
4. **Engine invocation**: Recommendation engine processes activities + criteria
5. **Result formatting**: API formats recommendations for client consumption
6. **Response**: JSON response with recommendations and score breakdowns

This pipeline maintains clear separation of concerns—the recommendation engine focuses purely on algorithmic logic while the API handles HTTP, validation, and data persistence.

## Design Evaluation

### Strengths

**Explainability**: Category-based scoring with detailed breakdowns allows teachers to understand recommendation rationale.

**Flexibility**: Priority system and configurable parameters adapt to diverse teaching contexts.

**Performance**: Two-stage scoring and combination limits enable interactive response times.

**Maintainability**: Clear separation of concerns and modular design facilitate testing and modification.

**No Training Data Required**: The system works immediately without historical data collection.

### Limitations

**Manual Weight Tuning**: Category impact weights were manually tuned rather than learned from data. Different weight configurations might improve results for specific use cases.

**Combination Limit**: Restricting to top-20 activities for combination generation may miss optimal multi-activity plans composed of medium-scoring activities.

**Linear Scoring**: Distance-based scoring (age, duration) uses linear decay. Non-linear functions might better capture pedagogical suitability.

**Static Break Rules**: Break calculations use fixed rules (10 minutes mental rest, 5 minutes format transition). Optimal break durations likely vary by context.

**No Learning**: The system doesn't learn from teacher selections.

### Trade-off Summary

The recommendation engine makes explicit trade-offs between competing concerns:

| Aspect | Choice | Alternative | Rationale |
|--------|--------|-------------|-----------|
| Algorithm | Category scoring | Collaborative filtering | No training data required, explainable |
| Aggregation | Weighted average | Min/max/product | Normalized scores, handles trade-offs |
| Performance | Two-stage scoring | Single-stage | Interactive response time critical |
| Combinations | Top-20 limit | All activities | Computational feasibility |
| Breaks | Automated calculation | User-specified | Reduces teacher cognitive load |
| Explainability | Category breakdown | Single score | Teacher agency and trust |

Each trade-off prioritizes prototype requirements (interactive performance, no training data, explainability) over production-system concerns (optimal accuracy, scalability to large datasets).
