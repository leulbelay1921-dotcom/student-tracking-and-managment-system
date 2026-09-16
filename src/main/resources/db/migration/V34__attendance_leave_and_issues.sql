-- =====================================================================
-- SITMS V34: Leave requests + attendance issues (SITMS-070)
--
-- Students request leave for internship days; the assigned supervisor
-- (company or academic) or an admin approves/rejects them. Students can
-- also report incorrect attendance rows via attendance_issue; the
-- reviewer responds while the row itself awaits manual correction.
-- Follows the audit-column and optimistic-lock conventions.
-- =====================================================================

CREATE TABLE leave_request (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID        NOT NULL REFERENCES student (id),
    internship_id UUID        NULL REFERENCES internship (id),
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    reason        TEXT        NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewer_id   UUID        NULL,
    review_note   TEXT        NULL,
    version       BIGINT      NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_leave_request_student ON leave_request (student_id);
CREATE INDEX idx_leave_request_status ON leave_request (status);
CREATE INDEX idx_leave_request_dates ON leave_request (start_date, end_date);

CREATE TABLE attendance_issue (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_id UUID        NOT NULL REFERENCES attendance (id),
    student_id    UUID        NOT NULL REFERENCES student (id),
    message       TEXT        NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewer_id   UUID        NULL,
    response      TEXT        NULL,
    version       BIGINT      NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attendance_issue_student ON attendance_issue (student_id);
CREATE INDEX idx_attendance_issue_status ON attendance_issue (status);
CREATE INDEX idx_attendance_issue_attendance ON attendance_issue (attendance_id);