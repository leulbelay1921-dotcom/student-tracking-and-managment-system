package com.sitms.internship;

/**
 * Lifecycle of an {@link Internship} (SITMS-018).
 *
 * <p>Transition rules are enforced in {@link InternshipService}: registered internships are
 * created as {@code PENDING}, are approved or rejected by an admin, and can be completed or
 * cancelled once active.
 */
public enum InternshipStatus {
  PENDING,
  APPROVED,
  REJECTED,
  COMPLETED,
  CANCELLED
}
