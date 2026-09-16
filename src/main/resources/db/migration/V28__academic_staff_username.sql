-- =====================================================================
-- SITMS V28: Short app-user id (username) on academic staff
--
-- Mirrors V26 for supervisors: the admin creates a faculty member by
-- typing a short id (e.g. fac001). The backend resolves-or-creates the
-- app_user identity and stores the short id here so the API can expose it.
-- =====================================================================

ALTER TABLE academic_staff ADD COLUMN username VARCHAR(64);

UPDATE academic_staff s
   SET username = au.username
  FROM app_user au
 WHERE au.id = s.app_user_id;

ALTER TABLE academic_staff ALTER COLUMN username SET NOT NULL;
ALTER TABLE academic_staff ALTER COLUMN username SET DEFAULT ('seed-' || gen_random_uuid()::text);

CREATE UNIQUE INDEX idx_academic_staff_username ON academic_staff (username);