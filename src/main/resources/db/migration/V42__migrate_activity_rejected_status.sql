-- =====================================================================
-- SITMS V42: Migrate legacy REJECTED status to COMPANY_REJECTED (SITMS-028)
--
-- Two-stage review (V39) introduced COMPANY_REJECTED and REVISION_REQUESTED
-- in place of the legacy REJECTED status. Migrate any remaining REJECTED
-- activity records to COMPANY_REJECTED.
-- =====================================================================

UPDATE activity SET status = 'COMPANY_REJECTED' WHERE status = 'REJECTED';
