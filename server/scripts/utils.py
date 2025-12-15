"""
Utility functions and constants for database setup scripts.
"""

from __future__ import annotations

import io
import logging
import secrets
import string

from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer

from app.core.models import ActivityResource, EnergyLevel
from app.db.models.activity import Activity
from app.db.models.user import User, UserRole
from app.services.user_service import UserService

logger = logging.getLogger(__name__)
admin_email = "admin@learn.hub"

# ============================================================================
# ADMIN USER MANAGEMENT
# ============================================================================


def generate_random_password(length: int = 16) -> str:
    """Generate a secure random password."""
    alphabet = string.ascii_letters + string.digits + "!@#$%^&*"
    return "".join(secrets.choice(alphabet) for _ in range(length))


def ensure_admin_user(session) -> str:
    """
    Ensure admin user exists with email admin@learnhub.
    Returns the password (either existing or newly generated).
    """
    user_service = UserService(session)

    # Check if admin user already exists
    admin_user = user_service.get_user_by_email(admin_email)

    if admin_user:
        logger.info(f"Admin user {admin_email} already exists")
        # Generate new password for existing user
        new_password = generate_random_password()
        user_service.set_password(admin_user.id, new_password)
        logger.info("Reset password for existing admin user")
        return new_password
    else:
        # Create new admin user
        admin_user = User(
            email=admin_email,
            first_name="Admin",
            last_name="User",
            role=UserRole.ADMIN,
        )
        session.add(admin_user)
        session.commit()

        # Set password
        password = generate_random_password()
        user_service.set_password(admin_user.id, password)
        logger.info(f"Created new admin user: {admin_email}")
        return password


# ============================================================================
# DEMO ACTIVITIES DATA
# ============================================================================


def get_demo_activities() -> list[Activity]:
    """Create a comprehensive set of demo activities with diverse series possibilities."""
    return [
        # === ALGORITHMS SERIES (Remember → Understand → Apply → Analyze → Evaluate) ===
        Activity(
            name="Algorithm Memory Game",
            source="CS Unplugged",
            age_min=8,
            age_max=12,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="remember",
            duration_min_minutes=15,
            duration_max_minutes=25,
            mental_load=EnergyLevel.LOW,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=3,
            cleanup_time_minutes=2,
            topics=["algorithms"],
        ),
        Activity(
            name="Algorithm Flowchart Basics",
            source="CS Unplugged",
            age_min=9,
            age_max=13,
            format="unplugged",
            resources_needed=["handouts", "stationery"],
            bloom_level="understand",
            duration_min_minutes=25,
            duration_max_minutes=35,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=3,
            topics=["algorithms"],
        ),
        Activity(
            name="Sorting Algorithm Practice",
            source="Code.org",
            age_min=10,
            age_max=14,
            format="digital",
            resources_needed=["computers"],
            bloom_level="apply",
            duration_min_minutes=30,
            duration_max_minutes=45,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=3,
            topics=["algorithms"],
        ),
        Activity(
            name="Algorithm Efficiency Analysis",
            source="Custom",
            age_min=12,
            age_max=16,
            format="digital",
            resources_needed=["computers"],
            bloom_level="analyze",
            duration_min_minutes=40,
            duration_max_minutes=55,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=8,
            cleanup_time_minutes=5,
            topics=["algorithms"],
        ),
        Activity(
            name="Algorithm Design Challenge",
            source="Robotics Lab",
            age_min=14,
            age_max=18,
            format="hybrid",
            resources_needed=["computers", "electronics"],
            bloom_level="evaluate",
            duration_min_minutes=60,
            duration_max_minutes=75,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.MEDIUM,
            prep_time_minutes=10,
            cleanup_time_minutes=8,
            topics=["algorithms"],
        ),
        # === PATTERNS SERIES (Remember → Understand → Apply → Create) ===
        Activity(
            name="Pattern Recognition Cards",
            source="CS Unplugged",
            age_min=6,
            age_max=10,
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
        ),
        Activity(
            name="Pattern Explanation Game",
            source="CS Unplugged",
            age_min=8,
            age_max=12,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="understand",
            duration_min_minutes=20,
            duration_max_minutes=30,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=3,
            cleanup_time_minutes=2,
            topics=["patterns"],
        ),
        Activity(
            name="Pattern Matching in Code",
            source="Code.org",
            age_min=10,
            age_max=14,
            format="digital",
            resources_needed=["computers"],
            bloom_level="apply",
            duration_min_minutes=25,
            duration_max_minutes=40,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=3,
            topics=["patterns"],
        ),
        Activity(
            name="Pattern Creation Workshop",
            source="MIT Scratch",
            age_min=12,
            age_max=16,
            format="digital",
            resources_needed=["computers"],
            bloom_level="create",
            duration_min_minutes=45,
            duration_max_minutes=60,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=8,
            cleanup_time_minutes=5,
            topics=["patterns"],
        ),
        # === ABSTRACTION SERIES (Understand → Apply → Analyze → Create) ===
        Activity(
            name="Abstraction Concept Introduction",
            source="CS Unplugged",
            age_min=10,
            age_max=14,
            format="unplugged",
            resources_needed=["handouts", "blocks"],
            bloom_level="understand",
            duration_min_minutes=30,
            duration_max_minutes=40,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=3,
            topics=["abstraction"],
        ),
        Activity(
            name="Function Abstraction Practice",
            source="Python.org",
            age_min=12,
            age_max=16,
            format="digital",
            resources_needed=["computers"],
            bloom_level="apply",
            duration_min_minutes=35,
            duration_max_minutes=50,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=8,
            cleanup_time_minutes=5,
            topics=["abstraction"],
        ),
        Activity(
            name="Abstraction Layer Analysis",
            source="Custom",
            age_min=14,
            age_max=18,
            format="digital",
            resources_needed=["computers"],
            bloom_level="analyze",
            duration_min_minutes=40,
            duration_max_minutes=55,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=10,
            cleanup_time_minutes=5,
            topics=["abstraction"],
        ),
        Activity(
            name="Abstraction Design Project",
            source="Robotics Lab",
            age_min=16,
            age_max=20,
            format="hybrid",
            resources_needed=["computers", "electronics"],
            bloom_level="create",
            duration_min_minutes=60,
            duration_max_minutes=90,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.MEDIUM,
            prep_time_minutes=15,
            cleanup_time_minutes=10,
            topics=["abstraction"],
        ),
        # === DECOMPOSITION SERIES (Understand → Apply → Analyze → Evaluate) ===
        Activity(
            name="Decomposition Story Problems",
            source="CS Unplugged",
            age_min=8,
            age_max=12,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="understand",
            duration_min_minutes=20,
            duration_max_minutes=30,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=3,
            cleanup_time_minutes=2,
            topics=["decomposition"],
        ),
        Activity(
            name="Step-by-Step Problem Solving",
            source="Code.org",
            age_min=10,
            age_max=14,
            format="digital",
            resources_needed=["computers"],
            bloom_level="apply",
            duration_min_minutes=25,
            duration_max_minutes=40,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=5,
            cleanup_time_minutes=3,
            topics=["decomposition"],
        ),
        Activity(
            name="Complex System Breakdown",
            source="Custom",
            age_min=12,
            age_max=16,
            format="hybrid",
            resources_needed=["computers", "blocks"],
            bloom_level="analyze",
            duration_min_minutes=35,
            duration_max_minutes=50,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.MEDIUM,
            prep_time_minutes=8,
            cleanup_time_minutes=5,
            topics=["decomposition"],
        ),
        Activity(
            name="Decomposition Strategy Evaluation",
            source="Robotics Lab",
            age_min=14,
            age_max=18,
            format="hybrid",
            resources_needed=["computers", "electronics"],
            bloom_level="evaluate",
            duration_min_minutes=45,
            duration_max_minutes=65,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.MEDIUM,
            prep_time_minutes=10,
            cleanup_time_minutes=8,
            topics=["decomposition"],
        ),
        # === CROSS-TOPIC SERIES (Multiple topics for complex series) ===
        Activity(
            name="Algorithm Pattern Recognition",
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
            topics=["algorithms", "patterns"],
        ),
        Activity(
            name="Pattern-Based Algorithm Design",
            source="Code.org",
            age_min=11,
            age_max=15,
            format="digital",
            resources_needed=["computers"],
            bloom_level="apply",
            duration_min_minutes=30,
            duration_max_minutes=45,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=8,
            cleanup_time_minutes=5,
            topics=["algorithms", "patterns"],
        ),
        Activity(
            name="Abstract Algorithm Implementation",
            source="Python.org",
            age_min=13,
            age_max=17,
            format="digital",
            resources_needed=["computers"],
            bloom_level="create",
            duration_min_minutes=40,
            duration_max_minutes=60,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=10,
            cleanup_time_minutes=5,
            topics=["algorithms", "abstraction"],
        ),
        # === SHORT DURATION SERIES (for quick lesson plans) ===
        Activity(
            name="Quick Pattern Match",
            source="CS Unplugged",
            age_min=7,
            age_max=11,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="remember",
            duration_min_minutes=5,
            duration_max_minutes=10,
            mental_load=EnergyLevel.LOW,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=1,
            cleanup_time_minutes=1,
            topics=["patterns"],
        ),
        Activity(
            name="Simple Decomposition",
            source="CS Unplugged",
            age_min=8,
            age_max=12,
            format="unplugged",
            resources_needed=["handouts"],
            bloom_level="understand",
            duration_min_minutes=10,
            duration_max_minutes=15,
            mental_load=EnergyLevel.LOW,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=2,
            cleanup_time_minutes=1,
            topics=["decomposition"],
        ),
        Activity(
            name="Basic Algorithm Steps",
            source="Code.org",
            age_min=9,
            age_max=13,
            format="digital",
            resources_needed=["computers"],
            bloom_level="apply",
            duration_min_minutes=15,
            duration_max_minutes=25,
            mental_load=EnergyLevel.MEDIUM,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=3,
            cleanup_time_minutes=2,
            topics=["algorithms"],
        ),
        # === LONG DURATION SERIES (for extended lesson plans) ===
        Activity(
            name="Comprehensive Algorithm Study",
            source="University Course",
            age_min=16,
            age_max=20,
            format="digital",
            resources_needed=["computers", "handouts"],
            bloom_level="analyze",
            duration_min_minutes=90,
            duration_max_minutes=120,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.LOW,
            prep_time_minutes=20,
            cleanup_time_minutes=10,
            topics=["algorithms", "abstraction"],
        ),
        Activity(
            name="Advanced Pattern Analysis",
            source="Research Lab",
            age_min=18,
            age_max=22,
            format="hybrid",
            resources_needed=["computers", "handouts"],
            bloom_level="evaluate",
            duration_min_minutes=75,
            duration_max_minutes=105,
            mental_load=EnergyLevel.HIGH,
            physical_energy=EnergyLevel.MEDIUM,
            prep_time_minutes=15,
            cleanup_time_minutes=8,
            topics=["patterns", "abstraction"],
        ),
        # === ENERGY BALANCE ACTIVITIES ===
        Activity(
            name="Physical Algorithm Walk",
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
        ),
        Activity(
            name="Mental Math Challenge",
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
        ),
    ]

# ============================================================================
# PDF UTILITIES
# ============================================================================


def create_placeholder_pdf() -> bytes:
    """Create a placeholder PDF for demo activities."""
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=letter)
    story = []

    # Add title
    styles = getSampleStyleSheet()
    title_style = styles["Heading1"]
    story.append(Paragraph("Demo Activity Placeholder", title_style))
    story.append(Spacer(1, 12))

    # Add description
    body_style = styles["BodyText"]
    story.append(
        Paragraph(
            "This is a placeholder PDF for demo activities in the LEARN-Hub system. "
            "This PDF is automatically generated and attached to all demo activities for testing purposes.",
            body_style,
        )
    )
    story.append(Spacer(1, 12))

    # Add note
    story.append(Paragraph("Please replace this with actual activity content as needed.", body_style))

    doc.build(story)
    buffer.seek(0)
    return buffer.getvalue()


# ============================================================================
# CSV PARSING UTILITIES
# ============================================================================


def parse_list(value: str) -> list[str]:
    """Parse pipe-separated list string."""
    if not value:
        return []
    return [item.strip() for item in value.split("|") if item.strip()]


def validate_resources(resources: list[str]) -> list[str]:
    """Filter resources to only include valid ActivityResource values."""
    valid_resources = {r.value for r in ActivityResource}
    validated = []
    for r in resources:
        if r.lower() in valid_resources:
            validated.append(r.lower())
        else:
            logger.warning(f"Ignoring invalid resource: {r}. Valid resources are: {valid_resources}")
    return validated
