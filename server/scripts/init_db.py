#!/usr/bin/env python3
"""
Database initialization script for Phase 1 PostgreSQL migration.
This script creates a fresh database with the new schema.
"""

import sys
from pathlib import Path

# Add the server directory to the Python path
server_dir = Path(__file__).parent.parent
sys.path.insert(0, str(server_dir))

from app.db.database import init_db
from app.utils.config import Config


def main():
    """Initialize the database with the new schema."""
    print("Initializing database...")

    # Load configuration
    config = Config()

    # Initialize database
    init_db()

    print("Database initialized successfully!")
    print(f"Database URI: {config.SQLALCHEMY_DATABASE_URI}")


if __name__ == "__main__":
    main()
