-- =====================================================================
-- SITMS V17: Attendance (SITMS-070)
--
-- A student records attendance during their internship via check-in /
-- check-out. status PRESENT | ABSENT follows the spec table (a row is
-- PRESENT once checked in, ABSENT when explicitly marked absent). Only
-- one row per student per date is allowed.
-- Follows the audit-column and optimistic-lock conventions from V1-V16.
-- =====================================================================

CREATE TABLE attendance (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID         NOT NULL REFERENCES student (id),
    internship_id   UUID         NULL REFERENCES internship (id),
    attendance_date DATE         NOT NULL,
    check_in        TIME         NULL,
    check_out       TIME         NULL,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PRESENT',
    note            TEXT         NULL,
    version         BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_attendance_student_date ON attendance (student_id, attendance_date);
CREATE INDEX idx_attendance_student ON attendance (student_id);
CREATE INDEX idx_attendance_internship ON attendance (internship_id);
CREATE INDEX idx_attendance_date ON attendance (attendance_date);