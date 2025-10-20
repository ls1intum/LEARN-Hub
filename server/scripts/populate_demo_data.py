#!/usr/bin/env python3
"""
Demo data population script for Phase 3.1.
This script populates the database with sample activities and users for demonstration.
"""

import sys
from pathlib import Path

# Add the server directory to the Python path
server_dir = Path(__file__).parent.parent
sys.path.insert(0, str(server_dir))

from app.core.models import EnergyLevel
from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.db.models.user import User, UserRole, PDFDocument
from app.services.user_service import UserService


def create_demo_activities():
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


def create_demo_users():
    """Create demo users for testing."""
    return [
        User(
            email="teacher@demo.com",
            first_name="Demo",
            last_name="Teacher",
            role=UserRole.TEACHER,
        ),
        User(
            email="admin@demo.com",
            first_name="Demo",
            last_name="Admin",
            role=UserRole.ADMIN,
        ),
    ]


def create_demo_pdf_document():
    """Create a demo PDF document for activities."""
    return PDFDocument(
        filename="demo_activities.pdf",
        file_path="/demo/path/activities.pdf",
        file_size=1024,
        extracted_fields={"demo": "data"},
        confidence_score="high",
        extraction_quality="excellent"
    )


def main():
    """Populate the database with demo data."""
    print("Populating database with demo data...")

    # Get database session
    session = get_db_session()

    try:
        # Check if data already exists
        existing_activities = session.query(Activity).count()
        existing_users = session.query(User).count()
        existing_documents = session.query(PDFDocument).count()

        # Create PDF document first (needed for activities)
        if existing_documents == 0:
            pdf_document = create_demo_pdf_document()
            session.add(pdf_document)
            session.flush()  # Flush to get the ID
            document_id = pdf_document.id
            print("Created demo PDF document")
        else:
            # Use existing document
            document_id = session.query(PDFDocument).first().id
            print(f"Using existing PDF document (ID: {document_id})")

        if existing_activities > 0:
            print(f"Database already contains {existing_activities} activities. Skipping activity creation.")
        else:
            # Create activities with document_id and description
            activities = create_demo_activities()
            for activity in activities:
                activity.document_id = document_id
                # Add default description if not set
                if not hasattr(activity, 'description') or not activity.description:
                    activity.description = f"Demo activity: {activity.name} - A comprehensive educational activity designed for students aged {activity.age_min}-{activity.age_max} years."
                session.add(activity)
            print(f"Created {len(activities)} demo activities")

        if existing_users > 0:
            print(f"Database already contains {existing_users} users. Skipping user creation.")
        else:
            # Create users
            users = create_demo_users()
            for user in users:
                session.add(user)
            print(f"Created {len(users)} demo users")

        # Commit changes
        session.commit()

        # Set password for admin user after commit
        user_service = UserService(session)
        admin_user = user_service.get_user_by_email("admin@demo.com")
        if admin_user:
            user_service.set_password(admin_user.id, "admin123")
            print("Set password for admin user")

        print("Demo data populated successfully!")
        print("\nDemo credentials:")
        print("- Teacher: teacher@demo.com (use magic link)")
        print("- Admin: admin@demo.com (password: admin123)")

    except Exception as e:
        session.rollback()
        print(f"Error populating demo data: {e}")
        raise
    finally:
        session.close()


if __name__ == "__main__":
    main()
