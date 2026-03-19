-- Add landscape orientation column to activity_markdowns
ALTER TABLE activity_markdowns ADD COLUMN landscape BOOLEAN NOT NULL DEFAULT false;

-- Set landscape=true for existing ARTIKULATIONSSCHEMA markdowns
UPDATE activity_markdowns SET landscape = true WHERE type = 'ARTIKULATIONSSCHEMA';
