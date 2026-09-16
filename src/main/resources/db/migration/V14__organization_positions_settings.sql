-- =====================================================================
-- SITMS V14: Organization positions + app settings (SITMS-012, SITMS-058)
--
-- Adds an optional per-organization position count used by the
-- organizations KPI cards, and introduces the app_setting table that
-- backs the admin Settings page with a small, typed key/value store.
-- App settings are seeded with the default internship/evaluation
-- configuration so the settings page is meaningful on first load.
-- =====================================================================

ALTER TABLE organization
    ADD COLUMN positions INTEGER NULL;

CREATE INDEX idx_organization_positions ON organization (positions);

CREATE TABLE app_setting (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key   VARCHAR(128)  NOT NULL UNIQUE,
    display_name  VARCHAR(255)  NOT NULL,
    setting_type  VARCHAR(32)   NOT NULL,
    setting_value VARCHAR(2000) NULL,
    description   VARCHAR(255)  NULL,
    version       BIGINT        NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

INSERT INTO app_setting (setting_key, display_name, setting_type, setting_value, description)
VALUES
    ('registration_open', 'Internship registration open', 'BOOLEAN', 'true',
     'Allow students to submit new internship applications.'),
    ('internship_default_duration_weeks', 'Default internship duration (weeks)', 'NUMBER', '12',
     'Default internship length in weeks when none is specified.'),
    ('min_weekly_hours', 'Minimum weekly internship hours', 'NUMBER', '20',
     'Minimum hours a student must log each week.'),
    ('evaluation_open', 'Evaluations open', 'BOOLEAN', 'true',
     'Allow supervisors to submit evaluations.');