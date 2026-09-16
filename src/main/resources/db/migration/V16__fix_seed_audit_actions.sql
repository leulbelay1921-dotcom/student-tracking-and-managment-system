-- =====================================================================
-- SITMS V16: Fix seeded audit actions + admin notifications
--
-- The V15 seed used a few audit_log.action values that are not valid
-- AuditAction enum constants (STUDENT_CREATED, ORGANIZATION_CREATED,
-- INTERNSHIP_APPROVED, TASK_ASSIGNED, ACTIVITY_APPROVED). This
-- migration rewrites those rows to valid enum values so reads of the
-- audit log do not fail, and adds a couple of notifications for the
-- admin user so the admin notification bell is not empty.
--
-- All statements are scoped by explicit ids and are safe no-ops on any
-- database that did not receive the V15 sample data.
-- =====================================================================

UPDATE audit_log
   SET action = 'INTERNSHIP_STATUS_CHANGE', detail = 'Approved internship for Student Test.'
 WHERE id = '86000000-0000-0000-0000-000000000001';

UPDATE audit_log
   SET action = 'DOCUMENT_UPLOADED', detail = 'Sarah Chen uploaded a document.'
 WHERE id = '86000000-0000-0000-0000-000000000002';

UPDATE audit_log
   SET action = 'INTERNSHIP_STATUS_CHANGE', detail = 'Approved internship for Sarah Chen.'
 WHERE id = '86000000-0000-0000-0000-000000000003';

UPDATE audit_log
   SET action = 'ACTIVITY_SUBMITTED', detail = 'Student Test submitted a new activity for review.'
 WHERE id = '86000000-0000-0000-0000-000000000004';

UPDATE audit_log
   SET action = 'ACTIVITY_REVIEWED', detail = 'Approved weekly activity from Student Test.'
 WHERE id = '86000000-0000-0000-0000-000000000005';

UPDATE audit_log
   SET action = 'EVALUATION_SUBMITTED', detail = 'Submitted evaluation for Student Test.'
 WHERE id = '86000000-0000-0000-0000-000000000006';

INSERT INTO notification (id, recipient_id, type, title, body, link, read, created_by) VALUES
    ('85000000-0000-0000-0000-000000000009', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'INTERNSHIP_STATUS_CHANGE', 'Internship approved', 'Internship for Student Test was approved at TechNova Solutions.',  '/internships', FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('85000000-0000-0000-0000-000000000010', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7', 'ACTIVITY_SUBMITTED',      'New activity submitted', 'Student Test submitted a new weekly activity for review.',         '/activities',  FALSE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7')
ON CONFLICT (id) DO NOTHING;