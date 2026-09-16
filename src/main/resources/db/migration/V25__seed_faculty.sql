-- =====================================================================
-- SITMS V25: Demo seed data for academic staff (faculty)
--
-- Adds the FACULTY Keycloak test user (faculty/faculty) as an app_user
-- and links it to an academic_staff row, mirroring V15. The app_user id
-- is the Keycloak user UUID so the frontend session maps 1:1.
--
-- Assigns the faculty member to the two TechNova internships so the
-- faculty dashboard shows "My Students" / "My Internships" demo data.
--
-- Guarded: only runs when academic_staff is empty, keeping the migration
-- idempotent for fresh and already-seeded environments.
-- =====================================================================

DO $seed$
BEGIN
  IF EXISTS (SELECT 1 FROM academic_staff) THEN
    RETURN;
  END IF;

  INSERT INTO app_user (id, username, email, display_name, role, active, created_by) VALUES
    ('30000000-0000-0000-0000-000000000001', 'faculty', 'faculty@sitms.local', 'Faculty Test', 'FACULTY', TRUE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, title, department, active, created_by) VALUES
    ('90000000-0000-0000-0000-000000000001', '30000000-0000-0000-0000-000000000001', 'FAC-0001', 'Faculty Test', 'Senior Lecturer', 'Software Engineering', TRUE, '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  UPDATE internship SET academic_staff_id = '90000000-0000-0000-0000-000000000001'
    WHERE id IN ('70000000-0000-0000-0000-000000000001', '70000000-0000-0000-0000-000000000002');

END
$seed$;