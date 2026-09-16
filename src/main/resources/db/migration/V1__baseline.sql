-- =====================================================================
-- SITMS baseline schema (V1)
--
-- Foundational infrastructure only. Feature-domain tables (students,
-- supervisors, organizations, internships, activities, tasks,
-- evaluations, documents, reports, notifications) are added by their
-- own feature migrations in later tickets.
--
-- Audit convention: every domain table carries these four columns:
--   created_by  UUID   NOT NULL  -> app_user.id
--   updated_by  UUID   NULL      -> app_user.id (null until first update)
--   created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
--   updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
-- Application code updates created_by/updated_at on writes; the DB
-- defaults cover inserts that originate outside the app.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Common identity for every principal (ADMIN / STUDENT / SUPERVISOR).
-- Feature tables reference this. Role membership lives on the subject
-- record or is derived from the auth provider (Keycloak) established in
-- SITMS-005.
CREATE TABLE app_user (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username      VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(128) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_app_user_role ON app_user (role);
CREATE INDEX idx_app_user_email ON app_user (email);
