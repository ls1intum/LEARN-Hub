#!/usr/bin/env python3
"""
Benchmark data population script.
Generates 10,000 diverse synthetic activities for performance testing.
"""

from __future__ import annotations

import random
import sys
from pathlib import Path

# Add the server directory to the Python path
server_dir = Path(__file__).parent.parent
sys.path.insert(0, str(server_dir))

from app.core.models import ActivityFormat, ActivityTopic, BloomLevel, EnergyLevel
from app.db.database import get_db_session
from app.db.models.activity import Activity
from app.db.models.user import PDFDocument


def generate_synthetic_activities(count: int = 10000) -> list[Activity]:
    """Generate diverse synthetic activities for benchmarking."""
    activities = []
    
    formats = [f.value for f in ActivityFormat]
    bloom_levels = [b.value for b in BloomLevel]
    topics = [t.value for t in ActivityTopic]
    resources = ["computers", "tablets", "handouts", "blocks", "electronics", "stationery"]
    energy_levels = [e.value for e in EnergyLevel]
    
    # Common activity name prefixes for variety
    prefixes = [
        "Algorithm",
        "Pattern",
        "Decomposition",
        "Abstraction",
        "Interactive",
        "Hands-on",
        "Digital",
        "Unplugged",
        "Hybrid",
        "Collaborative",
        "Individual",
        "Group",
    ]
    
    suffixes = [
        "Challenge",
        "Exercise",
        "Workshop",
        "Game",
        "Activity",
        "Project",
        "Lab",
        "Tutorial",
        "Exploration",
        "Practice",
        "Quest",
        "Simulation",
    ]
    
    sources = [
        "CS Unplugged",
        "Code.org",
        "MIT Scratch",
        "Python.org",
        "Custom",
        "Robotics Lab",
        "University Course",
        "Research Lab",
        "Online Platform",
        "Educational Journal",
    ]
    
    print(f"Generating {count:,} synthetic activities...")
    
    for i in range(count):
        # Random basic attributes
        age_min = random.randint(6, 12)
        age_max = random.randint(age_min, 15)
        format_val = random.choice(formats)
        bloom_val = random.choice(bloom_levels)
        
        # Duration: random between 5 and 120 minutes
        duration_min = random.randint(5, 100)
        duration_max = random.randint(duration_min, min(duration_min + 60, 120))
        
        # Random topics (1-3)
        activity_topics = random.sample(topics, random.randint(1, 3))
        
        # Random resources (0-4)
        num_resources = random.randint(0, 4)
        activity_resources = random.sample(resources, num_resources) if num_resources > 0 else []
        
        # Energy levels
        mental_load = random.choice(energy_levels)
        physical_energy = random.choice(energy_levels)
        
        # Prep and cleanup times (in 5-minute increments)
        prep_time = random.choice([0, 5, 10, 15, 20])
        cleanup_time = random.choice([0, 5, 10, 15])
        
        # Create activity name
        prefix = random.choice(prefixes)
        suffix = random.choice(suffixes)
        activity_name = f"{prefix} {suffix} {i % 100}"
        
        # Create description
        description = (
            f"Benchmark activity #{i + 1}. "
            f"This synthetic activity is designed for students aged {age_min}-{age_max} years "
            f"and covers topics including {', '.join(activity_topics)}. "
            f"The activity uses a {format_val} format and requires students to {bloom_val} concepts. "
            f"This is a randomly generated activity for performance testing purposes."
        )
        
        source = random.choice(sources)
        
        activity = Activity(
            name=activity_name,
            description=description,
            source=source,
            age_min=age_min,
            age_max=age_max,
            format=format_val,
            bloom_level=bloom_val,
            duration_min_minutes=duration_min,
            duration_max_minutes=duration_max,
            mental_load=EnergyLevel(mental_load),
            physical_energy=EnergyLevel(physical_energy),
            prep_time_minutes=prep_time,
            cleanup_time_minutes=cleanup_time,
            resources_needed=activity_resources,
            topics=activity_topics,
            document_id=1,  # Reuse document_id=1 to avoid creating many PDFs
        )
        
        activities.append(activity)
        
        # Print progress every 1000 activities
        if (i + 1) % 1000 == 0:
            print(f"  Generated {i + 1:,} activities...")
    
    return activities


def main():
    """Populate the database with synthetic benchmark data."""
    print("Starting benchmark data population...\n")
    
    session = get_db_session()
    
    try:
        # Check if data already exists
        existing_count = session.query(Activity).count()
        
        if existing_count > 0:
            print(f"Database already contains {existing_count:,} activities.")
            response = input("Do you want to clear and repopulate? (y/n): ").strip().lower()
            if response == "y":
                print("Clearing existing activities...")
                session.query(Activity).delete()
                session.commit()
                print("Database cleared.\n")
            else:
                print("Exiting without modification.")
                return
        
        # Ensure placeholder PDF document exists
        existing_doc = session.query(PDFDocument).filter(PDFDocument.id == 1).first()
        if not existing_doc:
            print("Creating placeholder PDF document...")
            import io

            from reportlab.lib.pagesizes import letter
            from reportlab.lib.styles import getSampleStyleSheet
            from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer

            from app.services.pdf_service import PDFService
            
            # Create placeholder PDF
            buffer = io.BytesIO()
            doc = SimpleDocTemplate(buffer, pagesize=letter)
            story = []
            styles = getSampleStyleSheet()
            story.append(Paragraph("Benchmark Activity Placeholder", styles["Heading1"]))
            story.append(Spacer(1, 12))
            story.append(
                Paragraph(
                    "This is a placeholder PDF for benchmark activities.",
                    styles["BodyText"],
                )
            )
            doc.build(story)
            buffer.seek(0)
            
            pdf_service = PDFService()
            doc_id = pdf_service.store_pdf(buffer.getvalue(), "benchmark_placeholder.pdf")
            print(f"Created PDF document with ID: {doc_id}\n")
        
        # Generate and insert activities
        activities = generate_synthetic_activities(10000)
        
        print("Inserting activities into database...")
        session.bulk_save_objects(activities)
        session.commit()
        
        final_count = session.query(Activity).count()
        print(f"\nSuccess! Database now contains {final_count:,} activities.\n")
        
    except Exception as e:
        session.rollback()
        print(f"Error populating database: {e}")
        raise
    finally:
        session.close()


if __name__ == "__main__":
    main()

