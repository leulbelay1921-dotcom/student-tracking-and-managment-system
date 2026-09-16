-- =====================================================================
-- SITMS V7: Activity + ActivityReview (SITMS-027)
--
-- A student logs daily/weekly internship activities which go through a
-- review workflow (submit -> approve/reject). ActivityReview stores the
-- reviewer decision and optional feedback for an activity (1:1).
-- Follows the audit-column and optimistic-lock conventions from V1-V6.
-- =====================================================================

CREATE TABLE activity (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID         NOT NULL REFERENCES student (id),
    internship_id  UUID         NULL REFERENCES internship (id),
    activity_date  DATE         NOT NULL,
    type           VARCHAR(16)  NOT NULL DEFAULT 'DAILY',
    description    TEXT         NOT NULL,
    hours          DOUBLE PRECISION NULL,
    status         VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    version        BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE activity_review (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID        NOT NULL UNIQUE REFERENCES activity (id),
    reviewer_id UUID        NOT NULL REFERENCES supervisor (id),
    status      VARCHAR(32) NOT NULL,
    feedback    TEXT        NULL,
    version     BIGINT      NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_activity_student ON activity (student_id);
CREATE INDEX idx_activity_internship ON activity (internship_id);
CREATE INDEX idx_activity_date ON activity (activity_date);
CREATE INDEX idx_activity_status ON activity (status);
CREATE INDEX idx_activity_review_reviewer ON activity_review (reviewer_id);
