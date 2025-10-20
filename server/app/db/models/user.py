from __future__ import annotations

import enum

from sqlalchemy import JSON, Column, DateTime, Enum, ForeignKey, Integer, String, func

from app.db.database import Base


class UserRole(enum.Enum):
    TEACHER = "TEACHER"
    ADMIN = "ADMIN"


class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    email = Column(String, unique=True, index=True, nullable=False)
    first_name = Column(String(100), nullable=False)
    last_name = Column(String(100), nullable=False)
    role = Column(Enum(UserRole), default=UserRole.TEACHER)
    password_hash = Column(String(255))  # For admin users and teachers with password login


class VerificationCode(Base):
    __tablename__ = "verification_codes"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    code = Column(String(6), nullable=False)  # 6-digit verification code
    expires_at = Column(DateTime, nullable=False)
    attempts = Column(Integer, default=0, nullable=False)  # Number of failed attempts
    used = Column(String(1), default="N", nullable=False)  # Y/N flag for used codes
    created_at = Column(DateTime, server_default=func.now())


class UserSearchHistory(Base):
    """Track user search queries from the recommendation form"""

    __tablename__ = "user_search_history"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    search_criteria = Column(JSON, nullable=False)  # Original search parameters
    created_at = Column(DateTime, server_default=func.now())


class UserFavourites(Base):
    """Track user hearted/favourited activities and lesson plans"""

    __tablename__ = "user_favourites"

    id = Column(Integer, primary_key=True, index=True)
    user_id = Column(Integer, ForeignKey("users.id"), nullable=False)
    favourite_type = Column(String(20), nullable=False)  # 'activity' or 'lesson_plan'
    activity_id = Column(Integer, nullable=True)  # For individual activity favourites
    activity_ids = Column(JSON, nullable=True)  # List of activity IDs for lesson plan favourites
    # Snapshot of the full lesson plan payload at time of favouriting (activities with inline breaks, metadata)
    lesson_plan_snapshot = Column(JSON, nullable=True)
    name = Column(String(255), nullable=True)  # Custom name for the favourite
    created_at = Column(DateTime, server_default=func.now())


class PDFDocument(Base):
    """Store PDF documents with extracted form fields"""

    __tablename__ = "pdf_documents"

    id = Column(Integer, primary_key=True, index=True)
    filename = Column(String(255), nullable=False)
    file_path = Column(String(500), nullable=False)
    file_size = Column(Integer, nullable=False)
    extracted_fields = Column(JSON, nullable=True)  # Extracted form fields from PDF
    confidence_score = Column(String(10), nullable=True)  # Extraction confidence
    extraction_quality = Column(String(20), nullable=True)  # Quality assessment
    created_at = Column(DateTime, server_default=func.now())
