-- Translate German markdown type identifiers to English.
-- activity_markdowns.type stores the MarkdownType enum name (EnumType.STRING).
UPDATE activity_markdowns SET type = 'COVER_SHEET' WHERE type = 'DECKBLATT';
UPDATE activity_markdowns SET type = 'LESSON_PLAN' WHERE type = 'ARTIKULATIONSSCHEMA';
UPDATE activity_markdowns SET type = 'BACKGROUND_KNOWLEDGE' WHERE type = 'HINTERGRUNDWISSEN';
UPDATE activity_markdowns SET type = 'BOARD_IMAGE' WHERE type = 'TAFELBILD';
UPDATE activity_markdowns SET type = 'EXERCISE' WHERE type = 'UEBUNG';
UPDATE activity_markdowns SET type = 'EXERCISE_SOLUTION' WHERE type = 'UEBUNG_LOESUNG';
