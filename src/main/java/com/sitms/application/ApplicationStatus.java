package com.sitms.application;

/**
 * Lifecycle of an {@link Application} (SITMS M2).
 *
 * <p>Applications start {@code PENDING}, are approved or rejected by an admin (or a supervisor of
 * the applicant) and can additionally be withdrawn by the owning student while pending. Approval
 * materializes the {@code Internship} placement; the other states are terminal.
 */
public enum ApplicationStatus {
  PENDING,
  APPROVED,
  REJECTED,
  WITHDRAWN
}