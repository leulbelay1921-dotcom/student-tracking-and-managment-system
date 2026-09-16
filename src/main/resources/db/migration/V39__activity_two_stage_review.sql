-- =====================================================================
-- SITMS V39: Two-stage activity review (SITMS-028)
--
-- Activities are reviewed in two stages. The company supervisor decides
-- on a SUBMITTED log (COMPANY_APPROVED|COMPANY_REJECTED); the university
-- supervisor then approves it (APPROVED) or requests a revision
-- (REVISION_REQUESTED). The company verdict stays in activity_review
-- (1:1, unique activity_id); the academic verdict is stored on the
-- activity row itself.
-- =====================================================================

ALTER TABLE activity ADD COLUMN academic_reviewer_id UUID;
ALTER TABLE activity ADD COLUMN academic_feedback TEXT;

CREATE INDEX idx_activity_academic_reviewer ON activity (academic_reviewer_id);