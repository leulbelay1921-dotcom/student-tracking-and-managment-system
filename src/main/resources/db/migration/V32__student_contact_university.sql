-- =====================================================================
-- SITMS V32: Student contact info, university and profile photo
--
-- Student-editable contact fields (phone, email) plus admin-controlled
-- official fields (university) and the profile photo (stored as a
-- data-URL string). Email is backfilled from app_user so existing
-- profiles have a value immediately.
-- =====================================================================

ALTER TABLE student ADD COLUMN university VARCHAR(128);
ALTER TABLE student ADD COLUMN phone VARCHAR(32);
ALTER TABLE student ADD COLUMN email VARCHAR(255);
ALTER TABLE student ADD COLUMN profile_photo TEXT;

UPDATE student s
SET email = au.email
FROM app_user au
WHERE au.id = s.app_user_id AND s.email IS NULL;