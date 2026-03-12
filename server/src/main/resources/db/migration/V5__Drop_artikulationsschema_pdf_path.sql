-- Remove unused artikulationsschema_pdf_path column
-- Artikulationsschema PDFs are generated on-the-fly from markdown, not stored on disk

ALTER TABLE activities DROP COLUMN IF EXISTS artikulationsschema_pdf_path;
