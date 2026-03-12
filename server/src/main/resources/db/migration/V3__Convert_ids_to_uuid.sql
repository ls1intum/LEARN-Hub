-- Migration to convert all ID columns from BIGINT to UUID
-- This enables use of UUIDs as primary keys and foreign keys across all tables

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Step 1: Add new UUID columns to all tables

-- Add UUID columns to users table
ALTER TABLE users ADD COLUMN id_uuid UUID DEFAULT uuid_generate_v4();

-- Add UUID columns to pdf_documents table
ALTER TABLE pdf_documents ADD COLUMN id_uuid UUID DEFAULT uuid_generate_v4();

-- Add UUID columns to activities table
ALTER TABLE activities ADD COLUMN id_uuid UUID DEFAULT uuid_generate_v4();
ALTER TABLE activities ADD COLUMN document_id_uuid UUID;

-- Add UUID columns to verification_codes table
ALTER TABLE verification_codes ADD COLUMN id_uuid UUID DEFAULT uuid_generate_v4();
ALTER TABLE verification_codes ADD COLUMN user_id_uuid UUID;

-- Add UUID columns to user_search_history table
ALTER TABLE user_search_history ADD COLUMN id_uuid UUID DEFAULT uuid_generate_v4();
ALTER TABLE user_search_history ADD COLUMN user_id_uuid UUID;

-- Add UUID columns to user_favourites table
ALTER TABLE user_favourites ADD COLUMN id_uuid UUID DEFAULT uuid_generate_v4();
ALTER TABLE user_favourites ADD COLUMN user_id_uuid UUID;
ALTER TABLE user_favourites ADD COLUMN activity_id_uuid UUID;

-- Step 2: Populate UUID foreign key columns based on existing relationships

-- Update activities.document_id_uuid based on document_id
UPDATE activities 
SET document_id_uuid = pdf_documents.id_uuid 
FROM pdf_documents 
WHERE activities.document_id = pdf_documents.id;

-- Update verification_codes.user_id_uuid based on user_id
UPDATE verification_codes 
SET user_id_uuid = users.id_uuid 
FROM users 
WHERE verification_codes.user_id = users.id;

-- Update user_search_history.user_id_uuid based on user_id
UPDATE user_search_history 
SET user_id_uuid = users.id_uuid 
FROM users 
WHERE user_search_history.user_id = users.id;

-- Update user_favourites.user_id_uuid based on user_id
UPDATE user_favourites 
SET user_id_uuid = users.id_uuid 
FROM users 
WHERE user_favourites.user_id = users.id;

-- Update user_favourites.activity_id_uuid based on activity_id
UPDATE user_favourites 
SET activity_id_uuid = activities.id_uuid 
FROM activities 
WHERE user_favourites.activity_id = activities.id;

-- Step 3: Drop old foreign key constraints
ALTER TABLE verification_codes DROP CONSTRAINT IF EXISTS verification_codes_user_id_fkey;
ALTER TABLE user_search_history DROP CONSTRAINT IF EXISTS user_search_history_user_id_fkey;
ALTER TABLE user_favourites DROP CONSTRAINT IF EXISTS user_favourites_user_id_fkey;

-- Step 4: Drop old columns and indexes

-- Drop old user columns and indexes
DROP INDEX IF EXISTS ix_users_id;
DROP INDEX IF EXISTS ix_users_email;
ALTER TABLE users DROP COLUMN IF EXISTS id;

-- Drop old pdf_documents columns and indexes
DROP INDEX IF EXISTS ix_pdf_documents_id;
ALTER TABLE pdf_documents DROP COLUMN IF EXISTS id;

-- Drop old activities columns
ALTER TABLE activities DROP COLUMN IF EXISTS id;
ALTER TABLE activities DROP COLUMN IF EXISTS document_id;

-- Drop old verification_codes columns and indexes
DROP INDEX IF EXISTS ix_verification_codes_id;
ALTER TABLE verification_codes DROP COLUMN IF EXISTS id;
ALTER TABLE verification_codes DROP COLUMN IF EXISTS user_id;

-- Drop old user_search_history columns and indexes
DROP INDEX IF EXISTS ix_user_search_history_id;
ALTER TABLE user_search_history DROP COLUMN IF EXISTS id;
ALTER TABLE user_search_history DROP COLUMN IF EXISTS user_id;

-- Drop old user_favourites columns and indexes
DROP INDEX IF EXISTS ix_user_favourites_id;
ALTER TABLE user_favourites DROP COLUMN IF EXISTS id;
ALTER TABLE user_favourites DROP COLUMN IF EXISTS user_id;
ALTER TABLE user_favourites DROP COLUMN IF EXISTS activity_id;

-- Step 5: Rename UUID columns to remove _uuid suffix

-- Rename users columns
ALTER TABLE users RENAME COLUMN id_uuid TO id;

-- Rename pdf_documents columns
ALTER TABLE pdf_documents RENAME COLUMN id_uuid TO id;

-- Rename activities columns
ALTER TABLE activities RENAME COLUMN id_uuid TO id;
ALTER TABLE activities RENAME COLUMN document_id_uuid TO document_id;

-- Rename verification_codes columns
ALTER TABLE verification_codes RENAME COLUMN id_uuid TO id;
ALTER TABLE verification_codes RENAME COLUMN user_id_uuid TO user_id;

-- Rename user_search_history columns
ALTER TABLE user_search_history RENAME COLUMN id_uuid TO id;
ALTER TABLE user_search_history RENAME COLUMN user_id_uuid TO user_id;

-- Rename user_favourites columns
ALTER TABLE user_favourites RENAME COLUMN id_uuid TO id;
ALTER TABLE user_favourites RENAME COLUMN user_id_uuid TO user_id;
ALTER TABLE user_favourites RENAME COLUMN activity_id_uuid TO activity_id;

-- Step 6: Add primary key constraints
ALTER TABLE users ADD PRIMARY KEY (id);
ALTER TABLE pdf_documents ADD PRIMARY KEY (id);
ALTER TABLE activities ADD PRIMARY KEY (id);
ALTER TABLE verification_codes ADD PRIMARY KEY (id);
ALTER TABLE user_search_history ADD PRIMARY KEY (id);
ALTER TABLE user_favourites ADD PRIMARY KEY (id);

-- Step 7: Add foreign key constraints
ALTER TABLE activities ADD CONSTRAINT activities_document_id_fkey FOREIGN KEY (document_id) REFERENCES pdf_documents(id);
ALTER TABLE verification_codes ADD CONSTRAINT verification_codes_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_search_history ADD CONSTRAINT user_search_history_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_favourites ADD CONSTRAINT user_favourites_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE user_favourites ADD CONSTRAINT user_favourites_activity_id_fkey FOREIGN KEY (activity_id) REFERENCES activities(id);

-- Step 8: Recreate indexes
CREATE UNIQUE INDEX ix_users_email ON users(email);
CREATE INDEX ix_users_id ON users(id);
CREATE INDEX ix_pdf_documents_id ON pdf_documents(id);
CREATE INDEX ix_verification_codes_id ON verification_codes(id);
CREATE INDEX ix_user_search_history_id ON user_search_history(id);
CREATE INDEX ix_user_favourites_id ON user_favourites(id);
