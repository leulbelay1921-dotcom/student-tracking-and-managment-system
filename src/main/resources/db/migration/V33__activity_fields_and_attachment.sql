-- =====================================================================
-- SITMS V33: Activity title/skills + shared Attachment link
--
-- Adds the student-facing fields (title, skills learned) to activity
-- logs. The attachment table links an already-stored document (whose
-- bytes live in document.content) to an owning record so activities and
-- weekly reports can surface uploaded files without duplicating them.
-- owner_type is ACTIVITY | WEEKLY_REPORT; owner_id is a plain UUID
-- reference to the owning row.
-- Follows the audit-column and optimistic-lock conventions.
-- =====================================================================

ALTER TABLE activity ADD COLUMN title VARCHAR(128) NULL;
ALTER TABLE activity ADD COLUMN skills TEXT NULL;

CREATE TABLE attachment (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_type  VARCHAR(32) NOT NULL,
    owner_id    UUID        NOT NULL,
    document_id UUID        NOT NULL REFERENCES document (id),
    version     BIGINT      NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachment_owner ON attachment (owner_type, owner_id);
CREATE INDEX idx_attachment_document ON attachment (document_id);
