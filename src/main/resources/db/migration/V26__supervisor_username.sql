-- =====================================================================
-- SITMS V26: Short app-user id (username) on supervisor
--
-- Mirrors V21 for students: the admin creates a supervisor by typing a
-- short id (e.g. sup001). The backend resolves-or-creates the app_user
-- identity and stores the short id here so the API can expose/search it.
-- =====================================================================

ALTER TABLE supervisor ADD COLUMN username VARCHAR(64);

UPDATE supervisor s
   SET username = au.username
  FROM app_user au
 WHERE au.id = s.app_user_id;

ALTER TABLE supervisor ALTER COLUMN username SET NOT NULL;
ALTER TABLE supervisor ALTER COLUMN username SET DEFAULT ('seed-' || gen_random_uuid()::text);

CREATE UNIQUE INDEX idx_supervisor_username ON supervisor (username);