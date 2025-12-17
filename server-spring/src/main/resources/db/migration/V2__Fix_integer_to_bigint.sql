-- Migration to fix column types from INTEGER to BIGINT
-- This migration is needed if you already ran V1 with SERIAL instead of BIGSERIAL

-- Alter activities table
ALTER TABLE activities ALTER COLUMN id TYPE BIGINT;
ALTER TABLE activities ALTER COLUMN document_id TYPE BIGINT;

-- Alter users table
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;

-- Alter pdf_documents table
ALTER TABLE pdf_documents ALTER COLUMN id TYPE BIGINT;

-- Alter verification_codes table
ALTER TABLE verification_codes ALTER COLUMN id TYPE BIGINT;
ALTER TABLE verification_codes ALTER COLUMN user_id TYPE BIGINT;

-- Alter user_search_history table
ALTER TABLE user_search_history ALTER COLUMN id TYPE BIGINT;
ALTER TABLE user_search_history ALTER COLUMN user_id TYPE BIGINT;

-- Alter user_favourites table
ALTER TABLE user_favourites ALTER COLUMN id TYPE BIGINT;
ALTER TABLE user_favourites ALTER COLUMN user_id TYPE BIGINT;
ALTER TABLE user_favourites ALTER COLUMN activity_id TYPE BIGINT;
