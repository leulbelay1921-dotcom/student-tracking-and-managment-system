-- =====================================================================
-- SITMS V4: Supervisor (SITMS-014)
--
-- Internship workplace supervisor. Extends the shared identity table
-- app_user (1:1 via app_user_id) and belongs to a hosting organization
-- (organization_id). Follows the audit-column and optimistic-lock
-- conventions established in V1-V3.
-- =====================================================================

CREATE TABLE supervisor (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_user_id     UUID         NOT NULL UNIQUE REFERENCES app_user (id),
    organization_id UUID         NOT NULL REFERENCES organization (id),
    employee_no     VARCHAR(32)  NOT NULL UNIQUE,
    full_name       VARCHAR(128) NOT NULL,
    job_title       VARCHAR(128) NULL,
    department      VARCHAR(128) NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_supervisor_organization ON supervisor (organization_id);
