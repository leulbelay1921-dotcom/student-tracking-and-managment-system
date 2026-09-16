ALTER TABLE supervisor ADD COLUMN phone VARCHAR(32);
ALTER TABLE supervisor ADD COLUMN email VARCHAR(255);

UPDATE supervisor s
SET email = au.email
FROM app_user au
WHERE au.id = s.app_user_id AND s.email IS NULL;