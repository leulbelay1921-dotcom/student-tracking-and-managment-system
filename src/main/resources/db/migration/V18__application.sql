-- =====================================================================
-- SITMS V18: Internship Application (SITMS M2)
--
-- A student's application for an internship placement. The application
-- captures the proposed placement (organization, title, period) and
-- travels PENDING -> APPROVED | REJECTED | WITHDRAWN. Approving an
-- application materializes the placement (an internship row, V5).
-- Audit-column and optimistic-lock conventions follow V1-V17.
-- =====================================================================

CREATE TABLE application (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID         NOT NULL REFERENCES student (id),
    organization_id UUID         NOT NULL REFERENCES organization (id),
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

CREATE INDEX idx_application_student ON application (student_id);
CREATE INDEX idx_application_organization ON application (organization_id);
CREATE INDEX idx_application_status ON application (status);