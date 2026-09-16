-- =====================================================================
-- SITMS V11: Audit log (SITMS-056)
--
-- Chronological, append-only record of security-relevant actions
-- (state transitions and review/approval decisions across internships,
-- activities, evaluations and documents). actor_name is the human
-- readable principal (JWT subject, e.g. an app_user UUID or the
-- non-UUID 'admin' system account); actor_id resolves to app_user.id
-- when the subject is a UUID, else NULL. action mirrors the domain
-- event, target_type/target_id identify the affected row, and detail
-- is a human-readable description. Follows the audit-column and
-- optimistic-lock conventions from V1-V10.
-- =====================================================================

CREATE TABLE audit_log (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id    UUID         NULL REFERENCES app_user (id),
    actor_name  VARCHAR(128) NOT NULL,
    action      VARCHAR(64)  NOT NULL,
    target_type VARCHAR(64)  NOT NULL,
    target_id   UUID         NULL,
    detail      VARCHAR(2000) NOT NULL,
    version     BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_created ON audit_log (created_at DESC);
CREATE INDEX idx_audit_log_actor ON audit_log (actor_id);
CREATE INDEX idx_audit_log_action ON audit_log (action);
CREATE INDEX idx_audit_log_target ON audit_log (target_type, target_id);