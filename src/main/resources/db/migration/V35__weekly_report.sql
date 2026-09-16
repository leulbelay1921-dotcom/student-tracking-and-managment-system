-- =====================================================================
-- SITMS V35: Weekly reports with two-stage approval (SITMS-077)
--
-- Students submit a report per internship week. The company supervisor
-- decides at stage one (PENDING_COMPANY -> COMPANY_APPROVED|REJECTED);
-- the academic staff gives the final verdict at stage two
-- (COMPANY_APPROVED -> APPROVED|REJECTED). Uploaded files ride along via
-- the generic attachment link table (owner_type = 'WEEKLY_REPORT').
-- =====================================================================

CREATE TABLE weekly_report (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id           UUID        NOT NULL REFERENCES student (id),
    internship_id        UUID        NULL REFERENCES internship (id),
    week_number          INTEGER     NOT NULL,
    title                VARCHAR(128) NULL,
    content              TEXT        NOT NULL,
    status               VARCHAR(24) NOT NULL DEFAULT 'PENDING_COMPANY',
    company_reviewer_id  UUID        NULL,
    company_review_note  TEXT        NULL,
    academic_reviewer_id UUID        NULL,
    academic_review_note TEXT        NULL,
    version              BIGINT      NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_weekly_report_student ON weekly_report (student_id);
CREATE INDEX idx_weekly_report_status ON weekly_report (status);
CREATE INDEX idx_weekly_report_student_week ON weekly_report (student_id, week_number);