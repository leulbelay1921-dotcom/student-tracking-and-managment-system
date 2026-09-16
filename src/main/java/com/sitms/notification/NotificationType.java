package com.sitms.notification;

/**
 * The domain event that produced a {@link Notification} (SITMS-050, EPIC 12).
 *
 * <p>The same type drives: (1) in-app grouping/filtering, and (2) email template selection in the
 * optional email channel (SITMS-053). Values are persisted as strings and must not be renamed.
 */
public enum NotificationType {
  INTERNSHIP_STATUS_CHANGE,
  APPLICATION_STATUS_CHANGE,
  ACTIVITY_SUBMITTED,
  ACTIVITY_REVIEWED,
  EVALUATION_SUBMITTED,
  DOCUMENT_UPLOADED,
  DOCUMENT_REVIEWED,
  LEAVE_REQUEST_SUBMITTED,
  LEAVE_REQUEST_REVIEWED,
  ATTENDANCE_ISSUE_REPORTED,
  ATTENDANCE_ISSUE_REVIEWED,
  WEEKLY_REPORT_SUBMITTED,
  WEEKLY_REPORT_COMPANY_APPROVED,
  WEEKLY_REPORT_APPROVED,
  WEEKLY_REPORT_REJECTED,
  WEEKLY_REPORT_CORRECTION_REQUESTED,
  TICKET_CREATED,
  TICKET_UPDATED,
  ACADEMIC_EVALUATION_SUBMITTED,
  ADMIN_BROADCAST
}