ALTER TABLE evaluation
  ADD COLUMN punctuality              SMALLINT NULL,
  ADD COLUMN reliability              SMALLINT NULL,
  ADD COLUMN independence_work        SMALLINT NULL,
  ADD COLUMN communication_skills     SMALLINT NULL,
  ADD COLUMN professional_conduct     SMALLINT NULL,
  ADD COLUMN learning_speed           SMALLINT NULL,
  ADD COLUMN commitment               SMALLINT NULL,
  ADD COLUMN accuracy                 SMALLINT NULL,
  ADD COLUMN team_contribution        SMALLINT NULL,
  ADD COLUMN cooperation              SMALLINT NULL,
  ADD COLUMN technical_proficiency    SMALLINT NULL,
  ADD COLUMN organizational_skills    SMALLINT NULL,
  ADD COLUMN project_support          SMALLINT NULL,
  ADD COLUMN task_responsibility      SMALLINT NULL,
  ADD COLUMN quality_team_member      SMALLINT NULL,
  ADD COLUMN remarks                  TEXT         NULL,
  ADD COLUMN department               VARCHAR(128) NULL,
  ADD COLUMN company_name             VARCHAR(255) NULL;

ALTER TABLE evaluation ADD CONSTRAINT chk_eval_punctuality   CHECK (punctuality   BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_reliability   CHECK (reliability   BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_independence_work CHECK (independence_work BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_comm_skills   CHECK (communication_skills BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_prof_conduct  CHECK (professional_conduct BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_learning_speed CHECK (learning_speed BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_commitment    CHECK (commitment    BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_accuracy      CHECK (accuracy      BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_team_contrib  CHECK (team_contribution BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_cooperation   CHECK (cooperation   BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_tech_prof     CHECK (technical_proficiency BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_org_skills    CHECK (organizational_skills BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_proj_support  CHECK (project_support BETWEEN 0 AND 5);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_task_resp     CHECK (task_responsibility BETWEEN 0 AND 15);
ALTER TABLE evaluation ADD CONSTRAINT chk_eval_quality_team  CHECK (quality_team_member BETWEEN 0 AND 20);