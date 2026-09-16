package com.sitms.task;

import java.util.UUID;

/**
 * Computed progress of a student's (or an internship's) tasks (SITMS-026).
 *
 * <p>{@code percent} is 0.0 when there are no tasks, otherwise completed/total as a 0..100 value.
 */
public record TaskProgress(
    UUID studentId, UUID internshipId, long totalTasks, long completedTasks, long inProgressTasks,
    long pendingTasks, double percent) {

  static TaskProgress of(
      UUID studentId, UUID internshipId, long total, long completed, long inProgress, long pending) {
    double percent = total == 0 ? 0.0 : (completed * 100.0) / total;
    return new TaskProgress(studentId, internshipId, total, completed, inProgress, pending, percent);
  }
}
