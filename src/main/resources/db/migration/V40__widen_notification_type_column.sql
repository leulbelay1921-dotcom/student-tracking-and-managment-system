-- =====================================================================
-- SITMS V40: Widen notification.type column to accommodate longer enum
-- values (e.g. WEEKLY_REPORT_CORRECTION_REQUESTED = 34 chars).
-- =====================================================================

ALTER TABLE notification ALTER COLUMN type TYPE VARCHAR(64);