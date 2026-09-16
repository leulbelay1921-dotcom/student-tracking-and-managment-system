package com.sitms.notification;

/**
 * Recipient group for an {@link AdminBroadcastRequest} (POST /notifications/broadcast).
 *
 * <p>{@code STUDENT} and {@code ORGANIZATION} target a single entity supplied as
 * {@code recipientId}: the student profile id, or the organization id (which resolves to that
 * organization's company supervisors plus the students placed there).
 */
public enum Audience {
  ALL_STUDENTS,
  COMPANY_SUPERVISORS,
  UNIVERSITY_SUPERVISORS,
  STUDENT,
  ORGANIZATION
}