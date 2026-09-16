-- =====================================================================
-- SITMS V12: Task priority (SITMS-023)
--
-- Adds a priority column to task, matching the TaskPriority enum. The
-- DEFAULT 'MEDIUM' fills existing rows so the NOT NULL constraint is
-- satisfied; the entity field defaults to MEDIUM as well (ddl-auto=validate).
-- =====================================================================

ALTER TABLE task
    ADD COLUMN priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM';

CREATE INDEX idx_task_priority ON task (priority);