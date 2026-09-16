-- =====================================================================
-- SITMS V24: Student support tickets (SITMS-073)
--
-- Student-raised helpdesk/issue tickets against their internship. A
-- student raises a ticket; either assigned supervisor (company
-- supervisor or academic staff) or an admin can progress it through
-- OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED and optionally assign it to
-- a specific supervisor (assignee_type SUPERVISOR | FACULTY,
-- assignee_id references the matching profile table).
-- Follows the audit-column and optimistic-lock conventions.
-- =====================================================================

CREATE TABLE ticket (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id     UUID         NOT NULL REFERENCES student (id),
    internship_id  UUID         NOT NULL REFERENCES internship (id),
    title          VARCHAR(128) NOT NULL,
    description    TEXT         NOT NULL,
    priority       VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    status         VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    assignee_type  VARCHAR(16)  NULL,
    assignee_id    UUID         NULL,
    version        BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ticket_student ON ticket (student_id);
CREATE INDEX idx_ticket_internship ON ticket (internship_id);
CREATE INDEX idx_ticket_status ON ticket (status);
CREATE INDEX idx_ticket_assignee ON ticket (assignee_type, assignee_id);