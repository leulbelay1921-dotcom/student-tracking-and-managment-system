-- =====================================================================
-- SITMS V15: Demo seed data
--
-- Seeds realistic sample data for the SITMS feature tables so the
-- frontend has something to render on first load. The three Keycloak
-- test users (admin / student / supervisor) are linked to their app_user
-- rows by their exact Keycloak user UUIDs (the JWT `sub`), matching
-- SecurityContextAuditor/AppUserIdentity conventions.
--
-- The whole block is guarded: it only runs when `student` (and so all
-- feature tables) is empty, keeping the migration idempotent for both
-- fresh databases and environments that were already seeded.
-- =====================================================================

DO $seed$
BEGIN
  IF EXISTS (SELECT 1 FROM student) THEN
    RETURN;
  END IF;

  -- ------------------------------------------------------------------
  -- Shared identities (app_user). IDs for admin/student/supervisor are
  -- the Keycloak user UUIDs so the frontend session maps 1:1.
  -- ------------------------------------------------------------------
  INSERT INTO app_user (id, username, email, display_name, role, active, created_by) VALUES
    ('10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'admin',      'admin@sitms.local',      'Admin Test',       'ADMIN',      TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('e5f0779b-ac33-4dc8-b145-3ce42e6f8928', 'student',    'student@sitms.local',    'Student Test',     'STUDENT',    TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('1b888ca9-28fa-42e7-9b16-bc077944d856', 'supervisor', 'supervisor@sitms.local', 'Supervisor Test',  'SUPERVISOR', TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000002', 'sarah.chen', 'sarah.chen@sitms.local', 'Sarah Chen',       'STUDENT',    TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000003', 'michael.ok', 'michael.ok@sitms.local', 'Michael Okafor',   'STUDENT',    TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000004', 'aisha.bello','aisha.bello@sitms.local','Aisha Bello',      'STUDENT',    TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000005', 'daniel.kim', 'daniel.kim@sitms.local', 'Daniel Kim',       'STUDENT',    TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000006', 'fatima.musa','fatima.musa@sitms.local','Fatima Musa',      'STUDENT',    TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000007', 'david.john', 'david.john@sitms.local', 'David Johnson',    'SUPERVISOR', TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('20000000-0000-0000-0000-000000000008', 'grace.ade',  'grace.ade@sitms.local',  'Grace Adewale',    'SUPERVISOR', TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Organizations
  -- ------------------------------------------------------------------
  INSERT INTO organization (id, name, industry, contact_email, contact_phone, address, website, active, positions, created_by) VALUES
    ('40000000-0000-0000-0000-000000000001', 'TechNova Solutions',    'Software Development',  'hr@technova.example',    '+23412300001', '14 Innovation Drive, Lagos',     'https://technova.example',    TRUE, 30, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('40000000-0000-0000-0000-000000000002', 'GreenField Foods',      'Agriculture & Agritech', 'careers@greenfield.example','+23412300002', '8 Harvest Road, Kaduna',       'https://greenfield.example',  TRUE, 12, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('40000000-0000-0000-0000-000000000003', 'ApexBank Financial',    'Banking & Finance',     'jobs@apexbank.example',   '+23412300003', '22 Marina, Port Harcourt',      'https://apexbank.example',    TRUE, 20, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('40000000-0000-0000-0000-000000000004', 'BuildRight Construction','Construction & Engineering','info@buildright.example','+23412300004','5 Site Works, Abuja',           'https://buildright.example',  TRUE, 8,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Supervisors
  -- ------------------------------------------------------------------
  INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, job_title, department, active, created_by) VALUES
    ('50000000-0000-0000-0000-000000000001', '1b888ca9-28fa-42e7-9b16-bc077944d856', '40000000-0000-0000-0000-000000000001', 'SUP-1001', 'Supervisor Test',   'Software Engineering Lead', 'Engineering',     TRUE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('50000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000007', '40000000-0000-0000-0000-000000000002', 'SUP-1002', 'David Johnson',     'Operations Manager',        'Operations',      TRUE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('50000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000008', '40000000-0000-0000-0000-000000000003', 'SUP-1003', 'Grace Adewale',     'HR Manager',               'Human Resources', TRUE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Students
  -- ------------------------------------------------------------------
  INSERT INTO student (id, app_user_id, student_no, full_name, program, department, status, level, created_by) VALUES
    ('60000000-0000-0000-0000-000000000001', 'e5f0779b-ac33-4dc8-b145-3ce42e6f8928', 'S1001', 'Student Test',     'B.Sc. Computer Science',     'Software Engineering',  'ACTIVE',    4, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('60000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 'S1002', 'Sarah Chen',       'B.Sc. Computer Science',     'Software Engineering',  'ACTIVE',    3, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('60000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 'S1003', 'Michael Okafor',   'B.Sc. Information Systems',  'Software Engineering',  'ACTIVE',    3, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('60000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 'S1004', 'Aisha Bello',      'B.Sc. Software Engineering', 'Data Analytics',        'PENDING',   2, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('60000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005', 'S1005', 'Daniel Kim',       'B.Eng. Computer Engineering', 'Networking & Security', 'PENDING',   2, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('60000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000006', 'S1006', 'Fatima Musa',      'B.Sc. Computer Science',     'Software Engineering',  'COMPLETED', 5, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Internships
  -- ------------------------------------------------------------------
  INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, description, start_date, end_date, status, created_by) VALUES
    ('70000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Software Development Intern',   'Backend API development and testing with the engineering squad.',          '2026-06-01', '2026-08-28', 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('70000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Frontend Engineering Intern',   'Angular UI components, accessibility and responsive layout.',              '2026-06-01', '2026-08-28', 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('70000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', '40000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000002', 'IT Operations Intern',          'Infrastructure monitoring, alerting and runbook documentation.',           '2026-06-01', '2026-08-28', 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('70000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000004', '40000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000003', 'Data Analytics Intern',         'Data modelling, dashboards and business intelligence reporting.',          '2026-09-01', '2026-11-30', 'PENDING',   '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('70000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000005', '40000000-0000-0000-0000-000000000004', NULL,                           'Engineering Intern',            'Field support, site surveys and CAD drafting.',                            '2026-09-01', '2026-11-30', 'PENDING',   '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('70000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000006', '40000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'DevOps Intern',                 'Containerization, CI/CD pipeline and deployment automation.',              '2025-06-02', '2025-08-29', 'COMPLETED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Tasks
  -- ------------------------------------------------------------------
  INSERT INTO task (id, student_id, internship_id, supervisor_id, title, description, due_date, status, priority, created_by) VALUES
    ('80000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Implement REST API endpoints',   'Add CRUD endpoints for the students and tasks resources.',            '2026-07-15', 'IN_PROGRESS', 'HIGH',   '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 'Write API integration tests',    'Cover the new endpoints with Spring integration tests.',                '2026-07-25', 'PENDING',    'MEDIUM', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', 'Build dashboard components',     'Create the activity and task widgets for the student dashboard.',      '2026-07-20', 'IN_PROGRESS', 'URGENT', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', 'Run accessibility audit',        'Audit the app against WCAG 2.1 AA and fix the reported issues.',        '2026-07-30', 'PENDING',    'LOW',    '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', 'Monitor infrastructure alerts',  'Tune CPU/memory alert thresholds for the production cluster.',          '2026-07-18', 'COMPLETED',  'MEDIUM', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', 'Document operations runbooks',   'Write runbooks for common incident response scenarios.',                '2026-08-05', 'PENDING',    'LOW',    '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000001', 'Containerize microservice',      'Dockerize the notification service and verify the image size.',         '2025-07-10', 'COMPLETED',  'HIGH',   '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('80000000-0000-0000-0000-000000000008', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000001', 'Set up CI/CD pipeline',         'Configure build, test and deploy stages for the backend.',              '2025-08-01', 'COMPLETED',  'MEDIUM', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Activities + activity reviews
  -- ------------------------------------------------------------------
  INSERT INTO activity (id, student_id, internship_id, activity_date, type, description, hours, status, created_by) VALUES
    ('81000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '2026-06-10', 'DAILY',  'Implemented pagination for the students list endpoint.',                            7.5, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '2026-06-11', 'DAILY',  'Wrote unit tests for the activity and task services.',                              6.0, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '2026-06-15', 'WEEKLY', 'Weekly summary: REST API work, tests, and code review feedback.',                   12.0, 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '2026-06-12', 'DAILY',  'Built the student profile card component and improved its loading state.',            6.5, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000005', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '2026-06-16', 'DAILY',  'Adjusted sidebar breakpoints; overlap on tablet widths needs a fix.',                4.5, 'REJECTED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000006', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '2026-06-09', 'DAILY',  'Tuned CPU utilisation alert thresholds to reduce false positives.',                 5.0, 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000007', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '2026-06-14', 'WEEKLY', 'Weekly summary: alert tuning, runbook skeleton, and incident review attendance.',    11.0, 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000008', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', '2025-06-20', 'DAILY',  'Dockerized the notification service and shrank the image by 40%.',                  7.0, 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000009', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', '2025-07-02', 'DAILY',  'Refactored the CI build stage to use build caching.',                               6.5, 'APPROVED',  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('81000000-0000-0000-0000-000000000010', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '2026-06-17', 'DAILY',  'Researched WebAuthn/passkey options for the authentication flow.',                  4.0, 'DRAFT',     '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  INSERT INTO activity_review (id, activity_id, reviewer_id, status, feedback, created_by) VALUES
    ('82000000-0000-0000-0000-000000000001', '81000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000001', 'APPROVED', 'Great progress. Keep the summaries coming.',                '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('82000000-0000-0000-0000-000000000002', '81000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000002', 'APPROVED', 'Nice work on the alert tuning.',                            '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('82000000-0000-0000-0000-000000000003', '81000000-0000-0000-0000-000000000007', '50000000-0000-0000-0000-000000000002', 'APPROVED', 'Comprehensive weekly summary.',                              '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('82000000-0000-0000-0000-000000000004', '81000000-0000-0000-0000-000000000008', '50000000-0000-0000-0000-000000000001', 'APPROVED', 'Good containerization work.',                               '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Documents (small inline placeholder content)
  -- ------------------------------------------------------------------
  INSERT INTO document (id, student_id, internship_id, file_name, content_type, size, type, status, reviewer_id, review_note, content, created_by) VALUES
    ('83000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', 'offer-letter-sitms.pdf',  'application/pdf', 245760, 'OFFER_LETTER', 'APPROVED', '50000000-0000-0000-0000-000000000001', 'Offer letter verified.',  decode('4f66666572204c6574746572', 'hex'),   '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('83000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', 'cv-member.pdf',            'application/pdf', 102400, 'REPORT',       'PENDING',  NULL,                                                NULL,             decode('435620446f63756d656e74', 'hex'),       '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('83000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', 'daily-report-week3.docx', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 204800, 'REPORT', 'REJECTED', '50000000-0000-0000-0000-000000000002', 'Please use the approved report template.', decode('4461696c79205265706f7274', 'hex'), '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('83000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', 'final-report.pdf',         'application/pdf', 409600, 'REPORT',       'APPROVED', '50000000-0000-0000-0000-000000000001', 'Final report accepted.',    decode('46696e616c205265706f7274', 'hex'),       '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Evaluations
  -- ------------------------------------------------------------------
  INSERT INTO evaluation (id, student_id, internship_id, supervisor_id, technical_skills, communication, teamwork, problem_solving, professionalism, strengths, improvement, comments, overall_score, status, created_by) VALUES
    ('84000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000001', '50000000-0000-0000-0000-000000000001', 4, 5, 5, 5, 5, 'Strong coding ability and clear communication.', 'Document complex changes more thoroughly.',                'A pleasure to mentor; delivered consistently.',        96,  'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('84000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '50000000-0000-0000-0000-000000000001', 5, 5, 4, 4, 5, 'Excellent frontend craft and attention to detail.', 'Write more automated tests for UI behaviour.',           'Shares work early and asks good questions.',           92,  'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('84000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '50000000-0000-0000-0000-000000000002', 4, 4, 5, 4, 4, 'Reliable operator who learns quickly.',          'Improve written documentation style.',                  'Dependable presence during incidents.',                84,  'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('84000000-0000-0000-0000-000000000004', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', '50000000-0000-0000-0000-000000000001', 5, 5, 5, 5, 5, 'Outstanding DevOps instincts and ownership.',   'None.',                                              'Top performer of the cohort; shipped end to end.',     100, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Notifications
  -- ------------------------------------------------------------------
  INSERT INTO notification (id, recipient_id, type, title, body, link, read, created_by) VALUES
    ('85000000-0000-0000-0000-000000000001', 'e5f0779b-ac33-4dc8-b145-3ce42e6f8928', 'INTERNSHIP_STATUS_CHANGE', 'Internship approved',  'Your internship at TechNova Solutions was approved.',            '/internships',       FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000002', 'e5f0779b-ac33-4dc8-b145-3ce42e6f8928', 'ACTIVITY_REVIEWED',       'Activity approved',    'Your weekly activity was approved by your supervisor.',          '/activities',        FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000002', 'INTERNSHIP_STATUS_CHANGE', 'Internship approved',  'Your internship at TechNova Solutions was approved.',            '/internships',       FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000004', '1b888ca9-28fa-42e7-9b16-bc077944d856', 'ACTIVITY_SUBMITTED',      'New activity submitted','Student Test submitted a new activity for review.',               '/activities/review', FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000005', '1b888ca9-28fa-42e7-9b16-bc077944d856', 'DOCUMENT_UPLOADED',       'Document uploaded',    'Student Test uploaded a new document.',                           '/documents',         FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000003', 'EVALUATION_SUBMITTED',    'Evaluation completed', 'Your supervisor submitted an evaluation for your internship.',    '/evaluations/mine',  TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000007', 'e5f0779b-ac33-4dc8-b145-3ce42e6f8928', 'DOCUMENT_REVIEWED',       'Document approved',    'Your offer letter was approved.',                                '/documents',         FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000002', 'ACTIVITY_REVIEWED',       'Activity reviewed',    'Your activity was reviewed by your supervisor.',                 '/activities',        TRUE,  '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Audit log (sample entries)
  -- ------------------------------------------------------------------
  INSERT INTO audit_log (id, actor_id, actor_name, action, target_type, target_id, detail, created_by) VALUES
    ('86000000-0000-0000-0000-000000000001', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'admin', 'STUDENT_CREATED',      'STUDENT',      '60000000-0000-0000-0000-000000000001', 'Seeded student profile for Student Test.',      '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('86000000-0000-0000-0000-000000000002', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'admin', 'ORGANIZATION_CREATED', 'ORGANIZATION', '40000000-0000-0000-0000-000000000001', 'Created organization TechNova Solutions.',      '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('86000000-0000-0000-0000-000000000003', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'admin', 'INTERNSHIP_APPROVED',  'INTERNSHIP',   '70000000-0000-0000-0000-000000000001', 'Approved internship for Student Test.',         '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('86000000-0000-0000-0000-000000000004', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'admin', 'TASK_ASSIGNED',        'TASK',         '80000000-0000-0000-0000-000000000001', 'Assigned task to Student Test.',                '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('86000000-0000-0000-0000-000000000005', '1b888ca9-28fa-42e7-9b16-bc077944d856', 'supervisor', 'ACTIVITY_APPROVED', 'ACTIVITY',     '81000000-0000-0000-0000-000000000003', 'Approved weekly activity from Student Test.',   '1b888ca9-28fa-42e7-9b16-bc077944d856'),
    ('86000000-0000-0000-0000-000000000006', '1b888ca9-28fa-42e7-9b16-bc077944d856', 'supervisor', 'EVALUATION_SUBMITTED', 'EVALUATION',   '84000000-0000-0000-0000-000000000001', 'Submitted evaluation for Student Test.',        '1b888ca9-28fa-42e7-9b16-bc077944d856');

END
$seed$;