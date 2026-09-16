-- =====================================================================
-- SITMS V30: Review queue demo data
--
-- Replenishes the admin/supervisor review queues with fresh PENDING work
-- so the review pages render without manually creating and submitting
-- records first:
--   * 3 SUBMITTED activities (shown in /activities/review)
--   * 2 PENDING documents    (shown in /documents/review)
--
-- The block is guarded on fixed marker UUIDs so it is idempotent and can
-- be re-applied safely.
-- =====================================================================

DO $seed$
BEGIN
  IF EXISTS (SELECT 1 FROM activity WHERE id = '88000000-0000-0000-0000-000000000001') THEN
    RETURN;
  END IF;

  -- ------------------------------------------------------------------
  -- Activities awaiting review
  -- ------------------------------------------------------------------
  INSERT INTO activity (id, student_id, internship_id, activity_date, type, description, hours, status, created_by) VALUES
    ('88000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', '2026-09-08', 'DAILY',  'Refactored the review queue to use isolated status loads and fixed the empty-list bug.', 6.5, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('88000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000003', '70000000-0000-0000-0000-000000000003', '2026-09-09', 'WEEKLY', 'Weekly summary: dashboard widgets, review queue fixes and documentation updates.',          14.0, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('88000000-0000-0000-0000-000000000003', '60000000-0000-0000-0000-000000000004', '70000000-0000-0000-0000-000000000004', '2026-09-10', 'DAILY',  'Cleaned and modelled the internship dataset for the placement report.',                     5.5, 'SUBMITTED', '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');

  -- ------------------------------------------------------------------
  -- Documents awaiting review
  -- ------------------------------------------------------------------
  INSERT INTO document (id, student_id, internship_id, file_name, content_type, size, type, status, reviewer_id, review_note, content, created_by) VALUES
    ('89000000-0000-0000-0000-000000000001', '60000000-0000-0000-0000-000000000006', '70000000-0000-0000-0000-000000000006', 'final-report-draft.docx',     'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 102400, 'REPORT',       'PENDING', NULL, NULL, decode('46696e616c207265706f7274206472616674', 'hex'), '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7'),
    ('89000000-0000-0000-0000-000000000002', '60000000-0000-0000-0000-000000000002', '70000000-0000-0000-0000-000000000002', 'offer-letter-techcorp.pdf',   'application/pdf', 210944, 'OFFER_LETTER', 'PENDING', NULL, NULL, decode('4f66666572206c6574746572', 'hex'),          '10c5d6a0-c5c4-4bf9-bdf0-081092925dc7');
END
$seed$;