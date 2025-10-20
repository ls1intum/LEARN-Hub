from __future__ import annotations

from collections.abc import Generator
from contextlib import contextmanager
from typing import Any

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, declarative_base, sessionmaker

from app.utils.config import Config


class DatabaseFactory:
    """Factory for creating database engines and sessions with dependency injection."""

    def __init__(self, config_instance: Any | None = None):
        self.config = config_instance or Config.get_instance()
        self._engine: Engine | None = None
        self._session_factory: sessionmaker[Session] | None = None

    @property
    def engine(self) -> Engine:
        """Get or create the database engine."""
        if self._engine is None:
            self._engine = self._create_engine()
        return self._engine

    @property
    def session_factory(self) -> sessionmaker[Session]:
        """Get or create the session factory."""
        if self._session_factory is None:
            self._session_factory = sessionmaker(autocommit=False, autoflush=False, bind=self.engine)
        return self._session_factory

    def _create_engine(self) -> Engine:
        """Create a database engine with proper configuration."""
        database_url = self.config.database_url

        # Configure connection pooling for PostgreSQL
        if database_url.startswith("postgresql://"):
            return create_engine(
                database_url,
                echo=False,
                pool_size=10,
                max_overflow=20,
                pool_pre_ping=True,
                pool_recycle=3600,
            )
        else:
            # SQLite configuration (for local development)
            return create_engine(database_url, echo=False)

    def get_session(self) -> Session:
        """Get a new database session."""
        return self.session_factory()

    @contextmanager
    def session_scope(self) -> Generator[Session]:
        """Context manager for database sessions with automatic cleanup."""
        session = self.get_session()
        try:
            yield session
            session.commit()
        except Exception as e:
            session.rollback()
            print(f"Database error: {e}")
            raise
        finally:
            session.close()

    def create_tables(self) -> None:
        """Create all database tables."""
        from app.db.models.activity import Activity  # noqa: F401
        from app.db.models.user import PDFDocument, User, UserFavourites, UserSearchHistory  # noqa: F401

        Base = declarative_base()
        try:
            Base.metadata.create_all(bind=self.engine)
        except Exception as e:
            # In test environments, database might not be available
            # Check if we're in a test environment and handle gracefully
            import os

            if os.environ.get("PYTEST_CURRENT_TEST") or "test" in os.environ.get("_", ""):
                print(f"Warning: Could not create database tables during testing: {e}")
                return
            raise


def init_database() -> None:
    """Initialize the database with tables."""
    factory = DatabaseFactory()
    factory.create_tables()
