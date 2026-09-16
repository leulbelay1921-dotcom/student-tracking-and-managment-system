-- =====================================================================
-- SITMS V36: Task workflow fields (SITMS-024)
--
-- Aligns the task table with the company-supervisor assignment workflow:
--   - extra planning fields (start date, category, expected outcome)
--   - default lifecycle status becomes ASSIGNED (student starts/completes,
--     supervisor marks completed work REVIEWED)
-- =====================================================================

ALTER TABLE task ADD COLUMN start_date       DATE         NULL;
ALTER TABLE task ADD COLUMN category         VARCHAR(64)  NULL;
ALTER TABLE task ADD COLUMN expected_outcome VARCHAR(2000) NULL;

ALTER TABLE task ALTER COLUMN status SET DEFAULT 'ASSIGNED';