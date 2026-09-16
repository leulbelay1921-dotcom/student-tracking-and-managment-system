-- SITMS-086: track the student admission batch (intake year) for cohort filtering.
ALTER TABLE student ADD COLUMN batch INT;