-- =====================================================================
-- SITMS V9: Evaluation (SITMS-040)
--
-- A supervisor's assessment of a student's performance during an
-- internship (EPIC 10). The review form uses five fixed quantitative
-- dimensions, each rated 1-5 (Technical Skills, Communication,
-- Teamwork, Problem Solving, Professionalism), three optional
-- free-text fields (Strengths, Areas for Improvement, General
-- Comments), and a stored overall score (0-100) computed from the
-- dimension scores. Status tracks the form lifecycle (DRAFT before
-- submission, SUBMITTED once the supervisor submits).
-- Follows the audit-column and optimistic-lock conventions from V1-V8.
-- =====================================================================

CREATE TABLE evaluation (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id      UUID         NOT NULL REFERENCES student (id),
    internship_id   UUID         NULL REFERENCES internship (id),
    supervisor_id   UUID         NOT NULL REFERENCES supervisor (id),
    technical_skills   SMALLINT  NOT NULL,
    communication      SMALLINT  NOT NULL,
    teamwork           SMALLINT  NOT NULL,
    problem_solving    SMALLINT  NOT NULL,
    professionalism    SMALLINT  NOT NULL,
    strengths        TEXT         NULL,
    improvement      TEXT         NULL,
    comments         TEXT         NULL,
    overall_score    SMALLINT     NOT NULL DEFAULT 0,
    status           VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    version          BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_evaluation_technical_skills CHECK (technical_skills BETWEEN 1 AND 5),
    CONSTRAINT chk_evaluation_communication CHECK (communication BETWEEN 1 AND 5),
    CONSTRAINT chk_evaluation_teamwork CHECK (teamwork BETWEEN 1 AND 5),
    CONSTRAINT chk_evaluation_problem_solving CHECK (problem_solving BETWEEN 1 AND 5),
    CONSTRAINT chk_evaluation_professionalism CHECK (professionalism BETWEEN 1 AND 5),
    CONSTRAINT chk_evaluation_overall_score CHECK (overall_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_evaluation_student ON evaluation (student_id);
CREATE INDEX idx_evaluation_internship ON evaluation (internship_id);
CREATE INDEX idx_evaluation_supervisor ON evaluation (supervisor_id);
CREATE INDEX idx_evaluation_status ON evaluation (status);