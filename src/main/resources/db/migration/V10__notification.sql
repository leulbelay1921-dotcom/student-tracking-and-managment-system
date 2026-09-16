-- =====================================================================
-- SITMS V10: Notification (SITMS-050)
--
-- An in-app notification addressed to a single app_user (EPIC 12).
-- Type identifies the domain event that produced it and is reused by
-- the email channel (SITMS-053) for template selection. title/body are
-- the human-readable message, link is the frontend route the
-- notification deep-links to, and read marks read-state (default unread).
-- Follows the audit-column and optimistic-lock conventions from V1-V9.
-- =====================================================================

CREATE TABLE notification (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID         NOT NULL REFERENCES app_user (id),
    type         VARCHAR(32)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT         NULL,
    link         VARCHAR(255) NULL,
    read         BOOLEAN      NOT NULL DEFAULT FALSE,
    version      BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_notification_recipient ON notification (recipient_id);
CREATE INDEX idx_notification_recipient_read ON notification (recipient_id, read);
CREATE INDEX idx_notification_recipient_created ON notification (recipient_id, created_at DESC);
CREATE INDEX idx_notification_type ON notification (type);