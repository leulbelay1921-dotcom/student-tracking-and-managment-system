-- =====================================================================
-- SITMS V5: Internship (SITMS-018)
--
-- A student's internship placement. Links a student to a hosting
-- organization and an assigned supervisor. Lifecycle is tracked by
-- status (see InternshipStatus enum). Follows the audit-column and
-- optimistic-lock conventions established in V1-V4.
-- =====================================================================

CREATE TABLE internship (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID         NOT NULL REFERENCES student (id),
    organization_id UUID         NOT NULL REFERENCES organization (id),
    supervisor_id   UUID         NULL REFERENCES supervisor (id),
    title           VARCHAR(128) NOT NULL,
    description     TEXT         NULL,
    start_date      DATE         NULL,
    end_date        DATE         NULL,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    version         BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_internship_student ON internship (student_id);
CREATE INDEX idx_internship_organization ON internship (organization_id);
CREATE INDEX idx_internship_supervisor ON internship (supervisor_id);
CREATE INDEX idx_internship_status ON internship (status);
