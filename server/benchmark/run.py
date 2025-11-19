#!/usr/bin/env python3
"""
Benchmark runner script.
Measures API performance under various recommendation scenarios.
"""

from __future__ import annotations

import csv
import sys
import time
from pathlib import Path

# Add the server directory to the Python path
server_dir = Path(__file__).parent.parent
sys.path.insert(0, str(server_dir))

from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.main import create_app


def check_data_available(session) -> bool:
    """Check if benchmark data exists in database."""
    count = session.query(Activity).count()
    return count > 0


def run_benchmark_scenarios(app, session) -> list[dict]:
    """Run all benchmark scenarios and return results."""
    client = app.test_client()
    results = []
    
    # Base scenarios (will be run with and without breaks)
    base_scenarios = [
        # HIGH COST SCENARIOS (no/minimal filtering = largest candidate set, most combinations)
        {
            "name": "1. High Cost - Single Activity, No Filters",
            "num_activities": 1,
            "filters_complexity": "high",
            "params": {
                "limit": 1,
                "max_activity_count": 1,
            },
        },
        {
            "name": "2. High Cost - 5 Activities, No Filters",
            "num_activities": 5,
            "filters_complexity": "high",
            "params": {
                "limit": 10,
                "max_activity_count": 5,
            },
        },
        {
            "name": "3. High Cost - 5 Activities, Age Only",
            "num_activities": 5,
            "filters_complexity": "high",
            "params": {
                "limit": 10,
                "max_activity_count": 5,
                "target_age": 10,
            },
        },
        {
            "name": "4. High Cost - 5 Activities, Age + Format (Broad)",
            "num_activities": 5,
            "filters_complexity": "high",
            "params": {
                "limit": 10,
                "max_activity_count": 5,
                "target_age": 10,
                "format": "digital,hybrid,unplugged",
            },
        },
        # MEDIUM COST SCENARIOS (moderate filtering = medium candidate set)
        {
            "name": "5. Medium Cost - 4 Activities, Age + Format",
            "num_activities": 4,
            "filters_complexity": "medium",
            "params": {
                "limit": 10,
                "max_activity_count": 4,
                "target_age": 10,
                "format": "digital,hybrid",
            },
        },
        {
            "name": "6. Medium Cost - 4 Activities, Age + Topic",
            "num_activities": 4,
            "filters_complexity": "medium",
            "params": {
                "limit": 10,
                "max_activity_count": 4,
                "target_age": 9,
                "preferred_topics": "patterns,algorithms",
            },
        },
        {
            "name": "7. Medium Cost - 3 Activities, Age + Bloom + Topic",
            "num_activities": 3,
            "filters_complexity": "medium",
            "params": {
                "limit": 10,
                "max_activity_count": 3,
                "target_age": 11,
                "bloom_levels": "understand,apply,analyze",
                "preferred_topics": "decomposition",
            },
        },
        # LOW COST SCENARIOS (aggressive filtering = smallest candidate set, fewer combinations)
        {
            "name": "8. Low Cost - 3 Activities, Age + Format + Bloom + Topic",
            "num_activities": 3,
            "filters_complexity": "low",
            "params": {
                "limit": 10,
                "max_activity_count": 3,
                "target_age": 12,
                "format": "digital",
                "bloom_levels": "apply,analyze",
                "preferred_topics": "algorithms,abstraction",
            },
        },
        {
            "name": "9. Low Cost - 2 Activities, Multiple Filters + Resources",
            "num_activities": 2,
            "filters_complexity": "low",
            "params": {
                "limit": 10,
                "max_activity_count": 2,
                "target_age": 10,
                "format": "hybrid",
                "bloom_levels": "analyze,evaluate",
                "preferred_topics": "abstraction",
                "available_resources": "computers,blocks",
            },
        },
        {
            "name": "10. Low Cost - 1 Activity, Most Restrictive Filters",
            "num_activities": 1,
            "filters_complexity": "low",
            "params": {
                "limit": 1,
                "max_activity_count": 1,
                "target_age": 13,
                "format": "digital",
                "bloom_levels": "create",
                "preferred_topics": "algorithms",
                "available_resources": "computers,electronics",
            },
        },
    ]
    
    # Generate scenarios with and without breaks
    scenarios = []
    for base in base_scenarios:
        # Without breaks
        scenario_without = base.copy()
        scenario_without["name"] = base["name"] + " (No Breaks)"
        scenario_without["include_breaks"] = False
        scenario_without["params"] = base["params"].copy()
        scenarios.append(scenario_without)
        
        # With breaks (only for multi-activity scenarios)
        if base["num_activities"] > 1:
            scenario_with = base.copy()
            scenario_with["name"] = base["name"] + " (With Breaks)"
            scenario_with["include_breaks"] = True
            scenario_with["params"] = base["params"].copy()
            scenario_with["params"]["include_breaks"] = "true"
            scenarios.append(scenario_with)
    
    print("Running benchmark scenarios...\n")
    print("-" * 130)
    print(f"{'Scenario':<75} {'Avg Time (ms)':<15} {'Min (ms)':<12} {'Max (ms)':<12}")
    print("-" * 130)
    
    num_iterations = 50
    
    for scenario in scenarios:
        times = []
        
        for _ in range(num_iterations):
            # Make request to recommendations endpoint
            start = time.time()
            response = client.get(
                "/api/activities/recommendations",
                query_string=scenario["params"],
            )
            elapsed = (time.time() - start) * 1000  # Convert to milliseconds
            
            if response.status_code != 200:
                print(f"Error in scenario '{scenario['name']}': Status {response.status_code}")
                print(f"Response: {response.get_json()}")
                break
            
            times.append(elapsed)
        
        if len(times) == num_iterations:
            avg_time = sum(times) / len(times)
            min_time = min(times)
            max_time = max(times)
            
            print(
                f"{scenario['name']:<75} {avg_time:<15.2f} {min_time:<12.2f} {max_time:<12.2f}"
            )
            
            result = {
                "scenario_name": scenario["name"],
                "num_activities": scenario["num_activities"],
                "include_breaks": scenario["include_breaks"],
                "filters_complexity": scenario["filters_complexity"],
                "avg_response_time_ms": round(avg_time, 2),
                "min_response_time_ms": round(min_time, 2),
                "max_response_time_ms": round(max_time, 2),
                "iterations": num_iterations,
            }
            results.append(result)
    
    print("-" * 130)
    
    return results


def save_results_to_csv(results: list[dict], output_path: Path):
    """Save benchmark results to CSV file."""
    if not results:
        print("No results to save.")
        return
    
    fieldnames = [
        "scenario_name",
        "num_activities",
        "include_breaks",
        "filters_complexity",
        "avg_response_time_ms",
        "min_response_time_ms",
        "max_response_time_ms",
        "iterations",
    ]
    
    with open(output_path, "w", newline="") as csvfile:
        writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(results)
    
    print(f"\nResults saved to {output_path}")


def main():
    """Run the benchmark suite."""
    print("LEARN-Hub Benchmark Suite\n")
    
    app = create_app()
    session = get_db_session()
    
    try:
        # Check if data is available
        if not check_data_available(session):
            print("Error: No benchmark data found in database.")
            print("Please run: cd server && uv run python benchmark/populate.py")
            return
        
        activity_count = session.query(Activity).count()
        print(f"Found {activity_count:,} activities in database.\n")
        
        with app.app_context():
            results = run_benchmark_scenarios(app, session)
        
        # Save to CSV
        output_path = Path(__file__).parent / "run.csv"
        save_results_to_csv(results, output_path)
        
        print("\nBenchmark complete!")
        
    except Exception as e:
        print(f"Error running benchmark: {e}")
        raise
    finally:
        session.close()


if __name__ == "__main__":
    main()

