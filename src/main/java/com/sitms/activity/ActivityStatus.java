package com.sitms.activity;

/** The lifecycle state of an {@link Activity} (SITMS-027).
 *
 * <p>Review flows in two stages: the company supervisor decides on a SUBMITTED log
 * (COMPANY_APPROVED or COMPANY_REJECTED); the university supervisor then approves it (APPROVED) or
 * sends it back for revision (REVISION_REQUESTED), after which the student edits and resubmits it.
 */
public enum ActivityStatus {
  DRAFT,
  SUBMITTED,
  COMPANY_APPROVED,
  COMPANY_REJECTED,
  APPROVED,
  REVISION_REQUESTED,
  /** Legacy rejection status, mapped/migrated to COMPANY_REJECTED. */
  REJECTED
}
