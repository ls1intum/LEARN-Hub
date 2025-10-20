"""
Test data for recommendation engine tests.
Contains pre-configured activities and mock data for consistent testing.
"""

from app.core.models import EnergyLevel
from app.db.models.activity import Activity


def create_test_activities():
    """Create a comprehensive set of test activities for recommendation engine testing."""
    return [
        # Low complexity, unplugged activities
        Activity(
            name="Binary Search Tree Activity",
            description=(
                "Students learn about binary search trees through hands-on activities using paper and markers "
                "to understand tree structure and search algorithms."
            ),
            source="CS Unplugged",
            age_min=10,
            age_max=14,
            format="unplugged",
            resources_needed=["handouts", "stationery"],
            bloom_level="understand",
            duration_min_minutes=30,
            duration_max_minutes=45,
            mental_load=EnergyLevel.LOW,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=5,
            topics=["algorithms", "patterns"],
            document_id=1,
        ),
        Activity(
            name="Sorting Network Game",
            description=(
                "Students learn sorting algorithms through a physical game where they arrange themselves in order "
                "using network connections and comparison rules."
            ),
            source="Code.org",
            age_min=8,
            age_max=12,
            format="unplugged",
            resources_needed=[],
            bloom_level="apply",
            duration_min_minutes=20,
            duration_max_minutes=30,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=3,
            cleanup_time_minutes=2,
            topics=["algorithms"],
            document_id=1,
        ),
        # Medium complexity, digital activities
        Activity(
            name="Python Turtle Graphics",
            description=(
                "Students create digital art and geometric patterns using Python turtle graphics library, "
                "learning programming concepts through visual creation."
            ),
            source="Python.org",
            age_min=12,
            age_max=16,
            format="digital",
            resources_needed=["computers"],
            bloom_level="create",
            duration_min_minutes=45,
            duration_max_minutes=60,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=10,
            cleanup_time_minutes=5,
            topics=["abstraction", "patterns"],
            document_id=1,
        ),
        Activity(
            name="Decomposition Puzzles",
            description=(
                "Students break down complex problems into smaller parts using physical blocks and digital tools "
                "to understand decomposition principles."
            ),
            source="Custom",
            age_min=6,
            age_max=10,
            format="hybrid",
            resources_needed=["blocks", "tablets"],
            bloom_level="analyze",
            duration_min_minutes=15,
            duration_max_minutes=25,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.MEDIUM,
            prep_time_minutes=5,
            cleanup_time_minutes=5,
            topics=["decomposition"],
            document_id=1,
        ),
        # High complexity, physical activities
        Activity(
            name="Robot Dance Programming",
            description=(
                "Students program robots to perform dance routines, combining coding skills with physical movement "
                "and evaluating different programming approaches."
            ),
            source="Robotics Lab",
            age_min=14,
            age_max=18,
            format="hybrid",
            resources_needed=["electronics", "computers"],
            bloom_level="evaluate",
            duration_min_minutes=60,
            duration_max_minutes=90,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.HIGH,
            prep_time_minutes=15,
            cleanup_time_minutes=10,
            topics=["algorithms", "abstraction"],
            document_id=1,
        ),
        Activity(
            name="Memory Card Game",
            description=(
                "Students play a memory card game to recognize and remember patterns, developing pattern recognition "
                "skills through gameplay."
            ),
            source="CS Unplugged",
            age_min=7,
            age_max=11,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="remember",
            duration_min_minutes=10,
            duration_max_minutes=20,
            mental_load=EnergyLevel.LOW,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=2,
            cleanup_time_minutes=2,
            topics=["patterns"],
            document_id=1,
        ),
        # Additional activities for series testing
        Activity(
            name="Pattern Recognition Basics",
            description=(
                "Students identify and analyze patterns in various contexts using worksheets and hands-on materials "
                "to understand "
                "pattern recognition concepts."
            ),
            source="CS Unplugged",
            age_min=9,
            age_max=13,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="understand",
            duration_min_minutes=25,
            duration_max_minutes=35,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=3,
            topics=["patterns", "abstraction"],
            document_id=1,
        ),
        Activity(
            name="Advanced Pattern Creation",
            description=(
                "Students create complex digital patterns using programming tools, applying advanced pattern "
                "recognition "
                "and abstraction concepts."
            ),
            source="CS Unplugged",
            age_min=11,
            age_max=15,
            format="digital",
            resources_needed=["computers"],
            bloom_level="create",
            duration_min_minutes=40,
            duration_max_minutes=55,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=8,
            cleanup_time_minutes=5,
            topics=["patterns", "abstraction"],
            document_id=1,
        ),
        # Activities for energy balance testing
        Activity(
            name="Physical Algorithm Walk",
            description=(
                "Students physically walk through algorithm steps, using movement to understand how algorithms work "
                "through kinesthetic learning."
            ),
            source="CS Unplugged",
            age_min=8,
            age_max=12,
            format="unplugged",
            resources_needed=[],
            bloom_level="apply",
            duration_min_minutes=20,
            duration_max_minutes=30,
            mental_load=EnergyLevel.LOW,
            physical_energy=EnergyLevel.HIGH,
            prep_time_minutes=2,
            cleanup_time_minutes=2,
            topics=["algorithms"],
            document_id=1,
        ),
        Activity(
            name="Mental Math Challenge",
            description=(
                "Students solve complex mathematical problems using algorithmic thinking and abstraction, developing "
                "computational problem-solving skills."
            ),
            source="Math Integration",
            age_min=10,
            age_max=14,
            format="unplugged",
            resources_needed=["stationery"],
            bloom_level="analyze",
            duration_min_minutes=15,
            duration_max_minutes=25,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=3,
            cleanup_time_minutes=2,
            topics=["algorithms", "abstraction"],
            document_id=1,
        ),
    ]


def create_priority_test_criteria():
    """Create test criteria for priority constraint testing."""
    return {
        "age_priority": {"target_age": 12, "priority": {"age": True}},
        "topics_priority": {"topics": ["algorithms"], "priority": {"topics": True}},
        "duration_priority": {"target_duration": 45, "priority": {"duration": True}},
        "bloom_priority": {"bloom_level": ["create"], "priority": {"bloom_level": True}},
        "multi_priority": {"target_age": 12, "topics": ["algorithms"], "priority": {"age": True, "topics": True}},
    }


def create_series_test_criteria():
    """Create test criteria for series testing."""
    return {
        "basic_series": {"target_duration": 50, "allow_lesson_plans": True},
        "series_with_breaks": {"target_duration": 50, "allow_lesson_plans": True, "include_breaks": True},
        "long_series": {"target_duration": 120, "allow_lesson_plans": True, "max_activity_count": 4},
        "bloom_levels_test": {"target_age": 10, "bloom_levels": ["understand", "apply"]},
        "empty_bloom_levels_test": {"target_age": 10, "bloom_levels": []},
    }


def get_expected_break_patterns():
    """Get expected break patterns for testing."""
    return {
        "high_mental_break": {
            "duration": 10,
            "type": "mental_rest",
            "description": "Mental rest break after high cognitive load",
        },
        "high_physical_break": {
            "duration": 5,
            "type": "physical_rest",
            "description": "Physical rest break after high energy activity",
        },
        "format_transition": {
            "duration": 3,
            "type": "transition",
            "description": "Transition break from unplugged to digital",
        },
        "prep_time": {"type": "prep", "description": "Preparation time for"},
        "cleanup_time": {"type": "cleanup", "description": "Cleanup time for"},
    }
