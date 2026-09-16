-- =====================================================================
-- SITMS V6: Task (SITMS-023)
--
-- A task assigned to a student, created by a supervisor, optionally
-- scoped to an internship. Lifecycle is tracked by status (see
-- TaskStatus enum). Follows the audit-column and optimistic-lock
-- conventions established in V1-V5.
-- =====================================================================

CREATE TABLE task (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID         NOT NULL REFERENCES student (id),
    internship_id UUID         NULL REFERENCES internship (id),
    supervisor_id UUID         NOT NULL REFERENCES supervisor (id),
    title         VARCHAR(128) NOT NULL,
    description   TEXT         NULL,
    due_date      DATE         NULL,
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    version       BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_task_student ON task (student_id);
CREATE INDEX idx_task_internship ON task (internship_id);
CREATE INDEX idx_task_supervisor ON task (supervisor_id);
CREATE INDEX idx_task_status ON task (status);
