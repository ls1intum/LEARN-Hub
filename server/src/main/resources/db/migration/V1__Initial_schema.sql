-- Initial database schema migration
-- Based on Alembic migration: 1986b4a071aa_initial_migration.py

-- Create custom enum types
CREATE TYPE energylevel AS ENUM ('LOW', 'MEDIUM', 'HIGH');
CREATE TYPE userrole AS ENUM ('TEACHER', 'ADMIN');

-- Create activities table
CREATE TABLE activities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    source VARCHAR(255),
    age_min INTEGER NOT NULL,
    age_max INTEGER NOT NULL,
    format VARCHAR(50) NOT NULL,
    bloom_level VARCHAR(50) NOT NULL,
    duration_min_minutes INTEGER NOT NULL,
    duration_max_minutes INTEGER,
    mental_load energylevel NOT NULL DEFAULT 'MEDIUM',
    physical_energy energylevel NOT NULL DEFAULT 'MEDIUM',
    prep_time_minutes INTEGER NOT NULL DEFAULT 5,
    cleanup_time_minutes INTEGER NOT NULL DEFAULT 5,
    resources_needed JSONB,
    topics JSONB,
    document_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create performance indexes for activities
CREATE INDEX ix_activities_age_range ON activities(age_min, age_max);
CREATE INDEX ix_activities_format ON activities(format);
CREATE INDEX ix_activities_bloom_level ON activities(bloom_level);

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role userrole NOT NULL DEFAULT 'TEACHER',
    password_hash VARCHAR(255)
);

-- Create indexes for users
CREATE UNIQUE INDEX ix_users_email ON users(email);
CREATE INDEX ix_users_id ON users(id);

-- Create PDF documents table
CREATE TABLE pdf_documents (
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    extracted_fields JSONB,
    confidence_score VARCHAR(10),
    extraction_quality VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_pdf_documents_id ON pdf_documents(id);

-- Create verification codes table
CREATE TABLE verification_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    code VARCHAR(6) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0,
    used VARCHAR(1) NOT NULL DEFAULT 'N',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_verification_codes_id ON verification_codes(id);

-- Create user search history table
CREATE TABLE user_search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    search_criteria JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_user_search_history_id ON user_search_history(id);

-- Create user favourites table
CREATE TABLE user_favourites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    favourite_type VARCHAR(20) NOT NULL,
    activity_id BIGINT,
    activity_ids JSONB,
    lesson_plan_snapshot JSONB,
    name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX ix_user_favourites_id ON user_favourites(id);
