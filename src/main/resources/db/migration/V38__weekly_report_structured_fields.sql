-- =====================================================================
-- SITMS V38: Structured fields for weekly reports
--
-- The student form is made up of four sections (plus week number, title
-- and attachments): activities completed, problems encountered, skills
-- learned and next week's plan. The original free-text `content` column
-- is kept for backwards compatibility but is no longer the primary input.
-- =====================================================================

ALTER TABLE weekly_report ADD COLUMN activities_completed TEXT;
ALTER TABLE weekly_report ADD COLUMN problems_encountered TEXT;
ALTER TABLE weekly_report ADD COLUMN skills_learned TEXT;
ALTER TABLE weekly_report ADD COLUMN next_week_plan TEXT;