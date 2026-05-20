ALTER TABLE activity_markdowns ADD COLUMN updated_at TIMESTAMP;
UPDATE activity_markdowns SET updated_at = created_at;
