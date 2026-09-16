-- =====================================================================
-- SITMS V8: Document (SITMS-036)
--
-- A file a student submits (offer letter, report, evidence) as part of
-- their internship, subject to type/size validation (SITMS-037) and a
-- review workflow (PENDING -> APPROVED / REJECTED). Content is stored
-- inline (BYTEA) so the app has no external object storage dependency.
-- Follows the audit-column and optimistic-lock conventions from V1-V7.
-- =====================================================================

CREATE TABLE document (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id    UUID         NOT NULL REFERENCES student (id),
    internship_id UUID         NULL REFERENCES internship (id),
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(128) NOT NULL,
    size          BIGINT       NOT NULL DEFAULT 0,
    type          VARCHAR(32)  NOT NULL DEFAULT 'OTHER',
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    reviewer_id   UUID         NULL REFERENCES supervisor (id),
    review_note   TEXT         NULL,
    content       BYTEA        NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_document_student ON document (student_id);
CREATE INDEX idx_document_internship ON document (internship_id);
CREATE INDEX idx_document_status ON document (status);
CREATE INDEX idx_document_type ON document (type);