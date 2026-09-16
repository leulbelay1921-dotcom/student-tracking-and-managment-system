-- Repair the faculty identity/profile when V25 was skipped because another
-- academic staff record already existed, or when the seeded placeholder id
-- (30000000-0000-0000-0000-000000000001) no longer matches the Keycloak
-- subject. The Keycloak realm export pins the faculty user to
-- 52d4254a-a51d-4754-87ee-5016ecbff859, so a fresh database converges here.
DO $repair$
BEGIN
  UPDATE academic_staff
     SET app_user_id = '52d4254a-a51d-4754-87ee-5016ecbff859'
   WHERE staff_no = 'FAC-0001';

  DELETE FROM app_user
   WHERE username = 'faculty'
     AND id <> '52d4254a-a51d-4754-87ee-5016ecbff859';

  INSERT INTO app_user (id, username, email, display_name, role, active, created_by)
  VALUES (
    '52d4254a-a51d-4754-87ee-5016ecbff859',
    'faculty',
    'faculty@sitms.local',
    'Faculty Test',
    'FACULTY',
    TRUE,
    '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7')
  ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    display_name = EXCLUDED.display_name,
    role = EXCLUDED.role,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

  UPDATE internship
  SET academic_staff_id = '90000000-0000-0000-0000-000000000001'
  WHERE id IN (
    '70000000-0000-0000-0000-000000000001',
    '70000000-0000-0000-0000-000000000002');
END
$repair$;