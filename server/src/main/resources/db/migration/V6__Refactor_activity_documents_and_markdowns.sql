-- Migration to refactor activity-document and activity-markdown relationships
-- Activities now link to lists of documents and markdowns

-- Add activity_id and type columns to the existing pdf_documents table
ALTER TABLE pdf_documents ADD COLUMN activity_id UUID;
ALTER TABLE pdf_documents ADD COLUMN type VARCHAR(50);

-- Migrate data: set activity_id and type from the old activities.document_id FK.
-- Note: assumes each document belongs to at most one activity (1:1 in practice).
UPDATE pdf_documents SET activity_id = a.id, type = 'SOURCE_PDF'
FROM activities a WHERE a.document_id = pdf_documents.id;

-- Add FK constraint and index
ALTER TABLE pdf_documents ADD CONSTRAINT fk_pdf_documents_activity
    FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE;
CREATE INDEX ix_pdf_documents_activity_id ON pdf_documents(activity_id);

-- Create activity_markdowns table
CREATE TABLE activity_markdowns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_markdowns_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE
);
CREATE INDEX ix_activity_markdowns_activity_id ON activity_markdowns(activity_id);

-- Migrate existing artikulationsschema markdown from activities to activity_markdowns
INSERT INTO activity_markdowns (id, activity_id, type, content, created_at)
SELECT uuid_generate_v4(), id, 'ARTIKULATIONSSCHEMA', artikulationsschema_markdown, COALESCE(created_at, CURRENT_TIMESTAMP)
FROM activities
WHERE artikulationsschema_markdown IS NOT NULL AND artikulationsschema_markdown != '';

-- Drop old columns from activities table
ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_document_id_fkey;
ALTER TABLE activities DROP COLUMN IF EXISTS document_id;
ALTER TABLE activities DROP COLUMN IF EXISTS artikulationsschema_markdown;
