package com.sitms.task;

/** The lifecycle state of a {@link Task} (SITMS-023). */
public enum TaskStatus {
  ASSIGNED,
  PENDING,
  IN_PROGRESS,
  COMPLETED,
  REVIEWED,
  CANCELLED
}
