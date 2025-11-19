# LEARN-Hub Benchmark Suite

Performance benchmarking for the recommendation engine with 10,000 synthetic activities.

## Scripts

**`populate.py`** - Generates 10,000 diverse synthetic activities
```bash
cd server && uv run python benchmark/populate.py
```

**`run.py`** - Runs 19 benchmark scenarios (10 without breaks + 9 with breaks) × 50 iterations
```bash
cd server && uv run python benchmark/run.py
```

## Output Format

Results saved to `server/benchmark/run.csv`:

| Column | Description |
|--------|-------------|
| scenario_name | Scenario label (includes "No Breaks" or "With Breaks") |
| num_activities | Activities in recommendation |
| include_breaks | Whether breaks were calculated |
| filters_complexity | high/medium/low based on filter count |
| avg_response_time_ms | Average response time (ms) |
| min_response_time_ms | Minimum response time (ms) |
| max_response_time_ms | Maximum response time (ms) |
| iterations | Number of runs (50) |

## Quick Start

```bash
cd server
uv run python benchmark/populate.py  # Populate database
uv run python benchmark/run.py       # Run benchmarks
cat benchmark/run.csv                # View results
```

## Scenarios

10 base scenarios tested in two variants (with/without breaks):

**High Cost**: No filters, large candidate set (4 scenarios)
**Medium Cost**: Age+Format/Topic/Bloom filters (3 scenarios)
**Low Cost**: Multiple filters, small candidate set (3 scenarios)

Single-activity scenarios only run once (breaks don't apply to single activities).
