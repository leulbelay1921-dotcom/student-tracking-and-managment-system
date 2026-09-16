-- =====================================================================
-- SITMS V41: Academic Evaluation
--
-- The university (academic) supervisor's assessment of a student's
-- internship. In contrast to the company-side `evaluation` (V9), this
-- form is keyed to a single internship (one record per internship) and
-- rates six fixed dimensions, each 1-5 (Academic Learning, Problem
-- Solving, Documentation, Technical Development, Professionalism,
-- Final Presentation). `total_score` (0-100) is computed from the six
-- dimension scores (mean scaled to a percentage, rounded) and stored
-- like the company evaluation so reports and analytics need no
-- derivation at read time. `evaluator_id` references the academic
-- staff member who performed the review. Audit columns and the
-- optimistic-lock `version` follow the V1 convention.
-- =====================================================================

CREATE TABLE academic_evaluation (
    id                    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id            UUID         NOT NULL REFERENCES student (id),
    internship_id         UUID         NOT NULL REFERENCES internship (id) UNIQUE,
    evaluator_id          UUID         NOT NULL REFERENCES academic_staff (id),
    academic_learning     SMALLINT     NOT NULL,
    problem_solving       SMALLINT     NOT NULL,
    documentation         SMALLINT     NOT NULL,
    technical_development SMALLINT     NOT NULL,
    professionalism       SMALLINT     NOT NULL,
    final_presentation    SMALLINT     NOT NULL,
    total_score           SMALLINT     NOT NULL,
    remarks               TEXT         NULL,
    version               BIGINT       NOT NULL DEFAULT 0,

    created_by UUID        NOT NULL,
    updated_by UUID        NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_academic_eval_academic_learning CHECK (academic_learning BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_problem_solving CHECK (problem_solving BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_documentation CHECK (documentation BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_technical_development CHECK (technical_development BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_professionalism CHECK (professionalism BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_final_presentation CHECK (final_presentation BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_total_score CHECK (total_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_academic_eval_student ON academic_evaluation (student_id);
CREATE INDEX idx_academic_eval_internship ON academic_evaluation (internship_id);
CREATE INDEX idx_academic_eval_evaluator ON academic_evaluation (evaluator_id);