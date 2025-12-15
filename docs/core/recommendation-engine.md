# Recommendation Engine

## Overview

The recommendation engine functions as an autonomous subsystem, isolated from the server's web framework to ensure architectural independence and algorithm reusability. The engine implements content-based filtering, scoring activities based on their metadata attributes rather than user interaction patterns. This approach provides an explainable alternative to opaque collaborative filtering methods.

**Note**: For detailed scoring formulas, design rationale, and pedagogical principles, see the thesis (Section 5.3.3, Architecture chapter).

## Scoring Categories

| Category | Impact | Description |
|----------|--------|-------------|
| **Age Appropriateness** | 4 | Tolerance-based matching with linear decay for activities outside target range |
| **Bloom's Alignment** | 5 | Hierarchical matching recognising cognitive progression across six levels |
| **Topic Relevance** | 4 | Proportional coverage of requested computational thinking topics |
| **Duration Fit** | 3 | Symmetric penalty model for deviations from target duration |
| **Series Cohesion** | 3 | For lesson plans: Topic overlap and Bloom progression between consecutive activities |

**Aggregation**: total_score = Σ(category_score × impact) / Σ(impact)

## Processing Pipeline

### 1. Hard Filtering

Activities must pass filters for age (within ±2 years), format, duration constraints, and resource requirements before scoring. This eliminates incompatible candidates early.

### 2. Two-Stage Scoring

- **Stage 1**: Score all candidates by age, Bloom alignment, and topic; select top 25 by preliminary score
- **Stage 2**: Generate activity combinations from top 25 only; insert breaks; re-score with duration

This limits combinatorial complexity from O(n!) to O(25^k), achieving sub-three-second response times.

### 3. Lesson Plan Generation

Generates 2-5 activity sequences with:
- Combinatorial limit: Top 25 activities only
- Pedagogical coherence via series cohesion scoring
- Bloom progression enforcement (non-decreasing)
- Topic overlap between consecutive activities

## Break System

Automated break calculation using rule-based logic:

| Break Type | Duration | Trigger |
|-----------|----------|---------|
| **Cleanup** | Variable | Based on activity metadata |
| **Mental Rest** | 10 min | After high mental load activities |
| **Physical Rest** | 5 min | After high physical energy activities |
| **Format Transition** | 5 min | When activity format changes |

**Note**: Breaks never added after the final activity in a lesson plan.

## System Integration

The engine resides in `app/core/` as a standalone package:

| Module | Responsibility |
|--------|----------------|
| `app/core/engine.py` | Main recommendation algorithm orchestration |
| `app/core/scoring.py` | Individual category scoring functions |
| `app/core/constants.py` | Configurable scoring parameters and thresholds |
| `app/core/models.py` | Data models for search criteria and results |

### Engine Interface

- **Input**: `SearchCriteria`, `List[ActivityModel]`, configuration parameters
- **Output**: Ranked recommendations with detailed score breakdowns per category

## Performance Characteristics

- **Candidate Limit**: Top 25 activities before expensive sequence generation
- **Hard Filtering**: Reduces initial candidate pool before scoring
- **Two-Stage Pipeline**: Defers expensive duration/break logic to Stage 2
- **Integer Arithmetic**: Uses [0-100] integer scales for efficiency

For detailed performance benchmarks and analysis, see the thesis (Chapter 6, Validation).
