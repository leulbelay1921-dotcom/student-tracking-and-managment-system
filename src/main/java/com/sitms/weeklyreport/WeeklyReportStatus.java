package com.sitms.weeklyreport;

/** Lifecycle of a weekly report under the two-stage approval flow (SITMS-077). */
public enum WeeklyReportStatus {
  /** Submitted by the student; awaiting the company supervisor's decision. */
  PENDING_COMPANY,
  /** Approved at stage one; now awaiting the academic staff's final decision. */
  COMPANY_APPROVED,
  /** Approved by both stages. */
  APPROVED,
  /** Rejected at either stage. */
  REJECTED,
  /** Sent back for re-work: the academic staff asked for corrections without rejecting it. */
  CORRECTION_REQUESTED
}