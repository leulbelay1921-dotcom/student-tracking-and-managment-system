-- H2-compatible schema for SITMS tests (translates V1-V14 migrations)
-- Must match the Flyway PostgreSQL schema exactly so raw JdbcTemplate INSERTs work.

CREATE TABLE IF NOT EXISTS app_user (
    id            UUID PRIMARY KEY,
    username      VARCHAR(64)  NOT NULL UNIQUE,
    email         VARCHAR(255) NOT NULL UNIQUE,
    display_name  VARCHAR(128) NOT NULL,
    role          VARCHAR(32)  NOT NULL,
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by    UUID         NOT NULL,
    updated_by    UUID,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS student (
    id          UUID PRIMARY KEY,
    app_user_id UUID         NOT NULL UNIQUE,
    username    VARCHAR(64)  UNIQUE,
    student_no  VARCHAR(32)  NOT NULL UNIQUE,
    full_name   VARCHAR(128) NOT NULL,
    program     VARCHAR(128) NOT NULL,
    level       INT          NOT NULL DEFAULT 1,
    batch       INT,
    department  VARCHAR(128),
    university  VARCHAR(128),
    phone       VARCHAR(32),
    email       VARCHAR(255),
    profile_photo TEXT,
    status      VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  UUID         NOT NULL,
    updated_by  UUID,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_user FOREIGN KEY (app_user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS organization (
    id            UUID PRIMARY KEY,
    name          VARCHAR(128) NOT NULL UNIQUE,
    industry      VARCHAR(128),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(32),
    address       VARCHAR(255),
    website       VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    positions     INT,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_by    UUID         NOT NULL,
    updated_by    UUID,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supervisor (
    id              UUID PRIMARY KEY,
    app_user_id     UUID         NOT NULL UNIQUE,
    username        VARCHAR(64)  UNIQUE,
    organization_id UUID         NOT NULL,
    employee_no     VARCHAR(32)  NOT NULL UNIQUE,
    full_name       VARCHAR(128) NOT NULL,
    job_title       VARCHAR(128),
    department      VARCHAR(128),
    email           VARCHAR(255),
    phone           VARCHAR(32),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    updated_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_supervisor_user FOREIGN KEY (app_user_id) REFERENCES app_user(id),
    CONSTRAINT fk_supervisor_org FOREIGN KEY (organization_id) REFERENCES organization(id)
);

CREATE TABLE IF NOT EXISTS internship (
    id              UUID PRIMARY KEY,
    student_id      UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    supervisor_id      UUID,
    academic_staff_id  UUID,
    title           VARCHAR(128) NOT NULL,
    description     TEXT,
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    updated_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_internship_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_internship_org FOREIGN KEY (organization_id) REFERENCES organization(id),
    CONSTRAINT fk_internship_supervisor FOREIGN KEY (supervisor_id) REFERENCES supervisor(id)
);

CREATE TABLE IF NOT EXISTS task (
    id               UUID PRIMARY KEY,
    student_id       UUID         NOT NULL,
    internship_id    UUID,
    supervisor_id    UUID         NOT NULL,
    title            VARCHAR(128) NOT NULL,
    description      TEXT,
    start_date       DATE,
    due_date         DATE,
    category         VARCHAR(64),
    expected_outcome VARCHAR(2000),
    status           VARCHAR(32)  NOT NULL DEFAULT 'ASSIGNED',
    priority         VARCHAR(32)  NOT NULL DEFAULT 'MEDIUM',
    version          BIGINT       NOT NULL DEFAULT 0,
    created_by       UUID         NOT NULL,
    updated_by       UUID,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_task_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_task_internship FOREIGN KEY (internship_id) REFERENCES internship(id),
    CONSTRAINT fk_task_supervisor FOREIGN KEY (supervisor_id) REFERENCES supervisor(id)
);

CREATE TABLE IF NOT EXISTS activity (
    id             UUID PRIMARY KEY,
    student_id     UUID         NOT NULL,
    internship_id  UUID,
    activity_date  DATE         NOT NULL,
    type           VARCHAR(16)  NOT NULL DEFAULT 'DAILY',
    title          VARCHAR(128),
    description    TEXT         NOT NULL,
    skills         TEXT,
    hours          DOUBLE PRECISION,
    status         VARCHAR(32)  NOT NULL DEFAULT 'DRAFT',
    academic_reviewer_id UUID,
    academic_feedback    TEXT,
    version        BIGINT       NOT NULL DEFAULT 0,
    created_by     UUID         NOT NULL,
    updated_by     UUID,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_activity_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_activity_internship FOREIGN KEY (internship_id) REFERENCES internship(id)
);

CREATE TABLE IF NOT EXISTS activity_review (
    id          UUID PRIMARY KEY,
    activity_id UUID        NOT NULL,
    reviewer_id UUID        NOT NULL,
    status      VARCHAR(32) NOT NULL,
    feedback    TEXT,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_by  UUID        NOT NULL,
    updated_by  UUID,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_review_activity FOREIGN KEY (activity_id) REFERENCES activity(id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewer_id) REFERENCES supervisor(id)
);

CREATE TABLE IF NOT EXISTS document (
    id            UUID PRIMARY KEY,
    student_id    UUID         NOT NULL,
    internship_id UUID,
    file_name     VARCHAR(255) NOT NULL,
    content_type  VARCHAR(128) NOT NULL,
    size          BIGINT       NOT NULL DEFAULT 0,
    type          VARCHAR(32)  NOT NULL DEFAULT 'OTHER',
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    reviewer_id   UUID,
    review_note   TEXT,
    content       BYTEA        NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_by    UUID         NOT NULL,
    updated_by    UUID,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_document_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_document_internship FOREIGN KEY (internship_id) REFERENCES internship(id),
    CONSTRAINT fk_document_reviewer FOREIGN KEY (reviewer_id) REFERENCES supervisor(id)
);

CREATE TABLE IF NOT EXISTS attachment (
    id          UUID PRIMARY KEY,
    owner_type  VARCHAR(32) NOT NULL,
    owner_id    UUID        NOT NULL,
    document_id UUID        NOT NULL,
    version     BIGINT      NOT NULL DEFAULT 0,
    created_by  UUID        NOT NULL,
    updated_by  UUID,
    created_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attachment_document FOREIGN KEY (document_id) REFERENCES document(id)
);

CREATE TABLE IF NOT EXISTS evaluation (
    id              UUID PRIMARY KEY,
    student_id      UUID         NOT NULL,
    internship_id   UUID,
    supervisor_id   UUID         NOT NULL,
    technical_skills  SMALLINT   NOT NULL,
    communication     SMALLINT   NOT NULL,
    teamwork          SMALLINT   NOT NULL,
    problem_solving   SMALLINT   NOT NULL,
    professionalism   SMALLINT   NOT NULL,
    strengths        TEXT,
    improvement      TEXT,
    comments         TEXT,
    punctuality              SMALLINT,
    reliability              SMALLINT,
    independence_work        SMALLINT,
    communication_skills     SMALLINT,
    professional_conduct     SMALLINT,
    learning_speed           SMALLINT,
    commitment               SMALLINT,
    accuracy                 SMALLINT,
    team_contribution        SMALLINT,
    cooperation              SMALLINT,
    technical_proficiency    SMALLINT,
    organizational_skills    SMALLINT,
    project_support          SMALLINT,
    task_responsibility      SMALLINT,
    quality_team_member      SMALLINT,
    remarks                  TEXT,
    department               VARCHAR(128),
    company_name             VARCHAR(255),
    overall_score    SMALLINT   NOT NULL DEFAULT 0,
    status           VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    version          BIGINT      NOT NULL DEFAULT 0,
    created_by       UUID        NOT NULL,
    updated_by       UUID,
    created_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_eval_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_eval_internship FOREIGN KEY (internship_id) REFERENCES internship(id),
    CONSTRAINT fk_eval_supervisor FOREIGN KEY (supervisor_id) REFERENCES supervisor(id),
    CONSTRAINT chk_eval_ts CHECK (technical_skills BETWEEN 1 AND 5),
    CONSTRAINT chk_eval_comm CHECK (communication BETWEEN 1 AND 5),
    CONSTRAINT chk_eval_tw CHECK (teamwork BETWEEN 1 AND 5),
    CONSTRAINT chk_eval_ps CHECK (problem_solving BETWEEN 1 AND 5),
    CONSTRAINT chk_eval_prof CHECK (professionalism BETWEEN 1 AND 5),
    CONSTRAINT chk_eval_os CHECK (overall_score BETWEEN 0 AND 100),
    CONSTRAINT chk_eval_punct CHECK (punctuality BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_rel CHECK (reliability BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_indep CHECK (independence_work BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_comm_skills CHECK (communication_skills BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_prof_cond CHECK (professional_conduct BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_learn CHECK (learning_speed BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_commit CHECK (commitment BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_acc CHECK (accuracy BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_team_c CHECK (team_contribution BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_coop CHECK (cooperation BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_tech_p CHECK (technical_proficiency BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_org_s CHECK (organizational_skills BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_proj_s CHECK (project_support BETWEEN 0 AND 5),
    CONSTRAINT chk_eval_task_r CHECK (task_responsibility BETWEEN 0 AND 15),
    CONSTRAINT chk_eval_qual_t CHECK (quality_team_member BETWEEN 0 AND 20)
);

CREATE TABLE IF NOT EXISTS notification (
    id           UUID PRIMARY KEY,
    recipient_id UUID         NOT NULL,
    type         VARCHAR(64)  NOT NULL,
    title        VARCHAR(255) NOT NULL,
    body         TEXT,
    link         VARCHAR(255),
    read         BOOLEAN      NOT NULL DEFAULT FALSE,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_by   UUID         NOT NULL,
    updated_by   UUID,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_recipient FOREIGN KEY (recipient_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id          UUID PRIMARY KEY,
    actor_id    UUID,
    actor_name  VARCHAR(128)  NOT NULL,
    action      VARCHAR(64)   NOT NULL,
    target_type VARCHAR(64)   NOT NULL,
    target_id   UUID,
    detail      VARCHAR(2000) NOT NULL,
    version     BIGINT        NOT NULL DEFAULT 0,
    created_by  UUID          NOT NULL,
    updated_by  UUID,
    created_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id              UUID PRIMARY KEY,
    student_id      UUID         NOT NULL,
    internship_id   UUID,
    attendance_date DATE         NOT NULL,
    check_in        TIME,
    check_out       TIME,
    status          VARCHAR(16)  NOT NULL DEFAULT 'PRESENT',
    note            TEXT,
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    updated_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_attendance_internship FOREIGN KEY (internship_id) REFERENCES internship(id),
    CONSTRAINT uq_attendance_student_date UNIQUE (student_id, attendance_date)
);

CREATE TABLE IF NOT EXISTS leave_request (
    id            UUID PRIMARY KEY,
    student_id    UUID        NOT NULL,
    internship_id UUID,
    start_date    DATE        NOT NULL,
    end_date      DATE        NOT NULL,
    reason        TEXT        NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewer_id   UUID,
    review_note   TEXT,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_by    UUID        NOT NULL,
    updated_by    UUID,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leave_request_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_leave_request_internship FOREIGN KEY (internship_id) REFERENCES internship(id)
);

CREATE TABLE IF NOT EXISTS attendance_issue (
    id            UUID PRIMARY KEY,
    attendance_id UUID        NOT NULL,
    student_id    UUID        NOT NULL,
    message       TEXT        NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reviewer_id   UUID,
    response      TEXT,
    version       BIGINT      NOT NULL DEFAULT 0,
    created_by    UUID        NOT NULL,
    updated_by    UUID,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_attendance_issue_attendance FOREIGN KEY (attendance_id) REFERENCES attendance(id),
    CONSTRAINT fk_attendance_issue_student FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE IF NOT EXISTS weekly_report (
    id                   UUID PRIMARY KEY,
    student_id           UUID         NOT NULL,
    internship_id        UUID,
    week_number          INTEGER      NOT NULL,
    title                VARCHAR(128),
    content              TEXT         NOT NULL,
    activities_completed TEXT,
    problems_encountered TEXT,
    skills_learned       TEXT,
    next_week_plan       TEXT,
    status               VARCHAR(24)  NOT NULL DEFAULT 'PENDING_COMPANY',
    company_reviewer_id  UUID,
    company_review_note  TEXT,
    academic_reviewer_id UUID,
    academic_review_note TEXT,
    version              BIGINT       NOT NULL DEFAULT 0,
    created_by           UUID         NOT NULL,
    updated_by           UUID,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_weekly_report_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_weekly_report_internship FOREIGN KEY (internship_id) REFERENCES internship(id)
);

CREATE TABLE IF NOT EXISTS application (
    id              UUID PRIMARY KEY,
    student_id      UUID         NOT NULL,
    organization_id UUID         NOT NULL,
    title           VARCHAR(128) NOT NULL,
    description     TEXT,
    start_date      DATE,
    end_date        DATE,
    status          VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    version         BIGINT       NOT NULL DEFAULT 0,
    created_by      UUID         NOT NULL,
    updated_by      UUID,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_application_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_application_org FOREIGN KEY (organization_id) REFERENCES organization(id)
);

CREATE TABLE IF NOT EXISTS app_setting (
    id               UUID PRIMARY KEY,
    setting_key      VARCHAR(128)  NOT NULL UNIQUE,
    display_name     VARCHAR(255)  NOT NULL,
    setting_type     VARCHAR(32)   NOT NULL,
    setting_category VARCHAR(32)   NOT NULL DEFAULT 'GENERAL',
    setting_value    VARCHAR(2000),
    description      VARCHAR(255),
    version          BIGINT        NOT NULL DEFAULT 0,
    created_by       UUID          NOT NULL DEFAULT '00000000-0000-0000-0000-000000000001',
    updated_by       UUID,
    created_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seed default app settings (matches V14 + V19 + V20 migrations)
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000001', 'registration_open', 'Internship registration open', 'BOOLEAN', 'GENERAL', 'true', 'Allow students to submit new internship applications.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000002', 'internship_default_duration_weeks', 'Default internship duration (weeks)', 'NUMBER', 'GENERAL', '12', 'Default internship length in weeks when none is specified.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000003', 'min_weekly_hours', 'Minimum weekly internship hours', 'NUMBER', 'GENERAL', '20', 'Minimum hours a student must log each week.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000004', 'evaluation_open', 'Evaluations open', 'BOOLEAN', 'GENERAL', 'true', 'Allow supervisors to submit evaluations.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000005', 'appearance', 'Appearance', 'STRING', 'APPEARANCE', 'solid', 'Portal look: solid, frosted, or glass.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000006', 'portal_name', 'Portal name', 'STRING', 'GENERAL', 'SITMS', 'Name shown across the portal.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000007', 'email_notifications', 'Email notifications', 'BOOLEAN', 'NOTIFICATIONS', 'true', 'Send email notifications for portal activity.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000008', 'application_alerts', 'Application alerts', 'BOOLEAN', 'NOTIFICATIONS', 'true', 'Notify when application statuses change.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000009', 'pending_review_threshold_days', 'Review threshold (days)', 'NUMBER', 'NOTIFICATIONS', '7', 'Days before review reminders are sent.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000010', 'session_timeout_minutes', 'Session timeout (minutes)', 'NUMBER', 'SECURITY', '60', 'Idle minutes before a session expires.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000011', 'password_expiry_days', 'Password expiry (days)', 'NUMBER', 'SECURITY', '90', 'Days before users must change their passwords.');
INSERT INTO app_setting (id, setting_key, display_name, setting_type, setting_category, setting_value, description)
    VALUES ('00000000-0000-0000-0000-000000000012', 'mfa_required', 'Require MFA', 'BOOLEAN', 'SECURITY', 'false', 'Require multi-factor authentication for all users.');

CREATE TABLE IF NOT EXISTS academic_staff (
    id              UUID PRIMARY KEY,
    app_user_id     UUID         NOT NULL UNIQUE,
    username        VARCHAR(64)  UNIQUE,
    staff_no        VARCHAR(32)  NOT NULL UNIQUE,
    full_name       VARCHAR(128) NOT NULL,
    title       VARCHAR(128),
    department  VARCHAR(128),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_by  UUID         NOT NULL,
    updated_by  UUID,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_academic_staff_user FOREIGN KEY (app_user_id) REFERENCES app_user(id)
);

CREATE TABLE IF NOT EXISTS academic_evaluation (
    id                    UUID PRIMARY KEY,
    student_id            UUID         NOT NULL,
    internship_id         UUID         NOT NULL UNIQUE,
    evaluator_id          UUID         NOT NULL,
    academic_learning     SMALLINT     NOT NULL,
    problem_solving       SMALLINT     NOT NULL,
    documentation         SMALLINT     NOT NULL,
    technical_development SMALLINT     NOT NULL,
    professionalism       SMALLINT     NOT NULL,
    final_presentation    SMALLINT     NOT NULL,
    total_score           SMALLINT     NOT NULL,
    remarks               TEXT,
    version               BIGINT       NOT NULL DEFAULT 0,
    created_by            UUID         NOT NULL,
    updated_by            UUID,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_academic_eval_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_academic_eval_internship FOREIGN KEY (internship_id) REFERENCES internship(id),
    CONSTRAINT fk_academic_eval_evaluator FOREIGN KEY (evaluator_id) REFERENCES academic_staff(id),
    CONSTRAINT chk_academic_eval_learning CHECK (academic_learning BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_ps CHECK (problem_solving BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_doc CHECK (documentation BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_td CHECK (technical_development BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_prof CHECK (professionalism BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_slides CHECK (final_presentation BETWEEN 1 AND 5),
    CONSTRAINT chk_academic_eval_total CHECK (total_score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS ticket (
    id            UUID PRIMARY KEY,
    student_id    UUID         NOT NULL,
    internship_id UUID         NOT NULL,
    title         VARCHAR(128) NOT NULL,
    description   TEXT         NOT NULL,
    priority      VARCHAR(16)  NOT NULL DEFAULT 'MEDIUM',
    status        VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    assignee_type VARCHAR(16),
    assignee_id   UUID,
    version       BIGINT       NOT NULL DEFAULT 0,
    created_by    UUID         NOT NULL,
    updated_by    UUID,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_student FOREIGN KEY (student_id) REFERENCES student(id),
    CONSTRAINT fk_ticket_internship FOREIGN KEY (internship_id) REFERENCES internship(id)
);
