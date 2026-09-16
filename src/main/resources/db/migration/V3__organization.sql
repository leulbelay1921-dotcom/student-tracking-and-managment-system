-- =====================================================================
-- SITMS V3: Organization (SITMS-011)
--
-- Employer/internship host organization. Standalone table following the
-- audit-column and optimistic-lock conventions established in V1/V2.
-- =====================================================================

CREATE TABLE organization (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(128) NOT NULL UNIQUE,
    industry      VARCHAR(128) NULL,
    contact_email VARCHAR(255) NULL,
    contact_phone VARCHAR(32)  NULL,
    address       VARCHAR(255) NULL,
    website       VARCHAR(255) NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    version       BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_organization_industry ON organization (industry);
