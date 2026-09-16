-- =====================================================================
-- SITMS V21: Short app-user id (username) on student
--
-- Admins create students by typing a short id (e.g. fk001). The backend
-- generates the internal app_user UUID and stores the short id as the
-- app_user.username. This column is the denormalized copy on student so
-- the list API can search and expose it without per-row lookups.
-- =====================================================================

ALTER TABLE student ADD COLUMN username VARCHAR(64);

UPDATE student s
   SET username = au.username
  FROM app_user au
 WHERE au.id = s.app_user_id;

ALTER TABLE student ALTER COLUMN username SET NOT NULL;
ALTER TABLE student ALTER COLUMN username SET DEFAULT ('seed-' || gen_random_uuid()::text);

CREATE UNIQUE INDEX idx_student_username ON student (username);