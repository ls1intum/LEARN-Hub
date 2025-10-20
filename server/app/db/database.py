from __future__ import annotations

from typing import cast

from sqlalchemy import create_engine
from sqlalchemy.engine import Engine
from sqlalchemy.orm import Session, declarative_base, scoped_session, sessionmaker

from app.utils.config import Config
from app.utils.database_factory import init_database

# Legacy global variables for backward compatibility
engine: Engine | None = None
db_session: scoped_session[Session] | None = None
Base = declarative_base()


def initialize_db_components():
    """Initialize legacy database components for backward compatibility."""
    global engine, db_session
    if engine is None:  # Only initialize if not already initialized
        # Use global config instance
        config = Config.get_instance()

        # Configure connection pooling for PostgreSQL
        if config.SQLALCHEMY_DATABASE_URI.startswith("postgresql://"):
            engine = create_engine(
                config.SQLALCHEMY_DATABASE_URI,
                echo=False,
                pool_size=10,
                max_overflow=20,
                pool_pre_ping=True,
                pool_recycle=3600,
            )
        else:
            # SQLite configuration (for local development)
            engine = create_engine(config.SQLALCHEMY_DATABASE_URI, echo=False)

        db_session = scoped_session(sessionmaker(autocommit=False, autoflush=False, bind=engine))
        Base.query = db_session.query_property()


def get_engine() -> Engine:
    """Get the database engine (legacy function)."""
    if engine is None:
        initialize_db_components()
    assert engine is not None
    return engine


def get_db_session() -> scoped_session[Session]:
    """Get a database session (legacy function)."""
    if db_session is None:
        initialize_db_components()
    # help type checkers know this is initialized
    return cast(scoped_session[Session], db_session)


def init_db():
    """Initialize database (legacy function)."""
    # Use the new database factory for initialization
    init_database()

    # Also initialize legacy components for backward compatibility
    initialize_db_components()
