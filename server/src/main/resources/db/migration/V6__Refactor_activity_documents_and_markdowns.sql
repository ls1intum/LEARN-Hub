-- Migration to refactor activity-document and activity-markdown relationships
-- Activities now link to lists of documents and markdowns via separate tables

-- Create activity_documents join table
CREATE TABLE activity_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    activity_id UUID NOT NULL,
    document_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_documents_activity FOREIGN KEY (activity_id) REFERENCES activities(id) ON DELETE CASCADE,
    CONSTRAINT fk_activity_documents_document FOREIGN KEY (document_id) REFERENCES pdf_documents(id)
);
CREATE INDEX ix_activity_documents_activity_id ON activity_documents(activity_id);

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

-- Migrate existing document references from activities to activity_documents
INSERT INTO activity_documents (id, activity_id, document_id, type, created_at)
SELECT uuid_generate_v4(), id, document_id, 'SOURCE_PDF', COALESCE(created_at, CURRENT_TIMESTAMP)
FROM activities
WHERE document_id IS NOT NULL;

-- Migrate existing artikulationsschema markdown from activities to activity_markdowns
INSERT INTO activity_markdowns (id, activity_id, type, content, created_at)
SELECT uuid_generate_v4(), id, 'ARTIKULATIONSSCHEMA', artikulationsschema_markdown, COALESCE(created_at, CURRENT_TIMESTAMP)
FROM activities
WHERE artikulationsschema_markdown IS NOT NULL AND artikulationsschema_markdown != '';

-- Drop old columns from activities table
ALTER TABLE activities DROP CONSTRAINT IF EXISTS activities_document_id_fkey;
ALTER TABLE activities DROP COLUMN IF EXISTS document_id;
ALTER TABLE activities DROP COLUMN IF EXISTS artikulationsschema_markdown;
