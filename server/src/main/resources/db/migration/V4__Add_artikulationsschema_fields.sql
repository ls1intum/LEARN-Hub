-- Migration to add Artikulationsschema fields to activities table
-- Stores the generated/extracted Artikulationsschema markdown and its rendered PDF path

ALTER TABLE activities ADD COLUMN artikulationsschema_markdown TEXT;
ALTER TABLE activities ADD COLUMN artikulationsschema_pdf_path VARCHAR(500);
