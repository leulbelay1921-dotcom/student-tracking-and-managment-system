-- =====================================================================
-- SITMS V23: Academic supervisor on internship (SITMS-072)
--
-- Adds the second supervisor reference to an internship. The existing
-- supervisor_id is the (company) workplace supervisor; academic_staff_id
-- references the university-side academic supervisor (V22). Both are
-- nullable because a placement can be approved before supervisors are
-- assigned. Company supervisor references supervisor(id);
-- academic supervisor references academic_staff(id).
-- =====================================================================

ALTER TABLE internship ADD COLUMN academic_staff_id UUID NULL REFERENCES academic_staff (id);

CREATE INDEX idx_internship_academic_staff ON internship (academic_staff_id);