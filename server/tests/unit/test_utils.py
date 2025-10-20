"""
Utility tests - High-value functionality only.
Combines password generation, time utils, and constants testing.
"""

import re

from app.core.models import (
    ActivityFormat,
    ActivityResource,
    ActivityTopic,
    BloomLevel,
    EnergyLevel,
)
from app.utils.constants import (
    AGE_RANGE,
    BLOOM_LEVEL_OPTIONS,
    DEFAULT_ENERGY_LEVEL,
    DURATION_RANGE,
    ENERGY_LEVEL_OPTIONS,
    FORMAT_OPTIONS,
    RESOURCE_OPTIONS,
    TOPIC_OPTIONS,
)
from app.utils.password_generator import PasswordGenerator
from app.utils.time_utils import TIME_INCREMENT_MINUTES, round_up_to_nearest_5_minutes


class TestPasswordGenerator:
    """Test password generator core functionality."""

    def test_generate_teacher_password_basic(self):
        """Test teacher password generation meets basic requirements."""
        password = PasswordGenerator.generate_teacher_password()

        assert isinstance(password, str)
        assert len(password) == 16
        assert re.search(r"[a-zA-Z0-9]", password)  # Contains alphanumeric
        assert re.search(r"[!@#$%^&*]", password)  # Contains symbol
        assert "0" not in password  # No ambiguous chars
        assert "O" not in password
        assert "I" not in password
        assert "l" not in password

    def test_generate_teacher_password_uniqueness(self):
        """Test password uniqueness across multiple generations."""
        passwords = {PasswordGenerator.generate_teacher_password() for _ in range(50)}
        assert len(passwords) >= 45  # High uniqueness rate


class TestTimeUtils:
    """Test time utility functions."""

    def test_round_up_to_nearest_5_minutes_comprehensive(self):
        """Test time rounding with various inputs."""
        # Exact multiples
        assert round_up_to_nearest_5_minutes(5) == 5
        assert round_up_to_nearest_5_minutes(10) == 10
        assert round_up_to_nearest_5_minutes(15) == 15

        # Round up cases
        assert round_up_to_nearest_5_minutes(1) == 5
        assert round_up_to_nearest_5_minutes(6) == 10
        assert round_up_to_nearest_5_minutes(11) == 15

        # Edge cases
        assert round_up_to_nearest_5_minutes(0) == 0
        assert round_up_to_nearest_5_minutes(-1) == 0
        assert round_up_to_nearest_5_minutes(100) == 100
        assert round_up_to_nearest_5_minutes(101) == 105

    def test_time_increment_constant(self):
        """Test time increment constant."""
        assert TIME_INCREMENT_MINUTES == 5


class TestConstants:
    """Test utility constants for completeness and correctness."""

    def test_format_options_completeness(self):
        """Test format options contain all enum values."""
        expected_formats = [f.value for f in ActivityFormat]
        assert FORMAT_OPTIONS == expected_formats
        assert len(FORMAT_OPTIONS) == 3
        assert "unplugged" in FORMAT_OPTIONS
        assert "digital" in FORMAT_OPTIONS
        assert "hybrid" in FORMAT_OPTIONS

    def test_resource_options_completeness(self):
        """Test resource options contain all enum values."""
        expected_resources = [r.value for r in ActivityResource]
        assert RESOURCE_OPTIONS == expected_resources
        assert len(RESOURCE_OPTIONS) == 6

    def test_topic_options_completeness(self):
        """Test topic options contain all enum values."""
        expected_topics = [t.value for t in ActivityTopic]
        assert TOPIC_OPTIONS == expected_topics
        assert len(TOPIC_OPTIONS) == 4

    def test_bloom_level_options_completeness(self):
        """Test Bloom level options contain all enum values."""
        expected_blooms = [b.value for b in BloomLevel]
        assert BLOOM_LEVEL_OPTIONS == expected_blooms
        assert len(BLOOM_LEVEL_OPTIONS) == 6

    def test_energy_level_options_completeness(self):
        """Test energy level options contain all enum values."""
        expected_energy = [e.value for e in EnergyLevel]
        assert ENERGY_LEVEL_OPTIONS == expected_energy
        assert len(ENERGY_LEVEL_OPTIONS) == 3

    def test_range_constants_validity(self):
        """Test range constants are valid."""
        assert AGE_RANGE["min"] < AGE_RANGE["max"]
        assert DURATION_RANGE["min"] < DURATION_RANGE["max"]
        assert AGE_RANGE["min"] >= 0
        assert DURATION_RANGE["min"] >= 0

    def test_default_values_validity(self):
        """Test default values are valid."""
        assert DEFAULT_ENERGY_LEVEL in ENERGY_LEVEL_OPTIONS
