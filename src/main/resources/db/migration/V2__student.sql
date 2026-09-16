-- =====================================================================
-- SITMS V2: Student profile (SITMS-006)
--
-- The student profile extends the shared identity table app_user (V1)
-- with academic fields, joined 1:1 via app_user_id. The audit column
-- convention from V1 is followed exactly.
--
-- version is an optimistic-lock counter maintained by the app (@Version).
-- =====================================================================

CREATE TABLE student (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    app_user_id UUID         NOT NULL UNIQUE REFERENCES app_user (id),
    student_no  VARCHAR(32)  NOT NULL UNIQUE,
    full_name   VARCHAR(128) NOT NULL,
    program     VARCHAR(128) NOT NULL,
    level       INT          NOT NULL DEFAULT 1,
    version     BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_student_program ON student (program);
