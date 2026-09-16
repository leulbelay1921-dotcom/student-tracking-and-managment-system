-- =====================================================================
-- SITMS V13: Student department + status (SITMS-007)
--
-- Extends the student profile with an internship-track department and
-- lifecycle status (ACTIVE / PENDING / COMPLETED) to match the SITMS
-- student-management design columns. Department is optional; status
-- defaults to PENDING for existing rows.
-- =====================================================================

ALTER TABLE student
    ADD COLUMN department VARCHAR(128) NULL,
    ADD COLUMN status     VARCHAR(32)  NOT NULL DEFAULT 'PENDING';

CREATE INDEX idx_student_department ON student (department);
