-- =====================================================================
-- SITMS V22: Academic staff (faculty) (SITMS-072)
--
-- University-side academic supervisor. Mirrors the workplace supervisor
-- (V4) shape but is not tied to a hosting organization: the academic
-- supervisor is a staff/faculty member who supervises the student's
-- internship from the university side. Extends app_user (1:1) and uses
-- the standard audit-column and optimistic-lock conventions.
-- =====================================================================

CREATE TABLE academic_staff (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_user_id UUID         NOT NULL UNIQUE REFERENCES app_user (id),
    staff_no    VARCHAR(32)  NOT NULL UNIQUE,
    full_name   VARCHAR(128) NOT NULL,
    title       VARCHAR(128) NULL,
    department  VARCHAR(128) NULL,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);