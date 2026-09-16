package com.sitms.evaluation;

/**
 * Lifecycle of an {@link Evaluation}: a {@code DRAFT} is being filled in by the supervisor, a
 * {@code SUBMITTED} evaluation is final and visible to the student.
 */
public enum EvaluationStatus {
  DRAFT,
  SUBMITTED
}