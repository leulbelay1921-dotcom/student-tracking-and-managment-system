package com.sitms.task;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link Task}. */
public record TaskResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    UUID supervisorId,
    String title,
    String description,
    LocalDate startDate,
    LocalDate dueDate,
    String category,
    String expectedOutcome,
    TaskStatus status,
    TaskPriority priority,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static TaskResponse from(Task task) {
    return new TaskResponse(
        task.getId(),
        task.getStudentId(),
        task.getInternshipId(),
        task.getSupervisorId(),
        task.getTitle(),
        task.getDescription(),
        task.getStartDate(),
        task.getDueDate(),
        task.getCategory(),
        task.getExpectedOutcome(),
        task.getStatus(),
        task.getPriority(),
        task.getCreatedAt(),
        task.getUpdatedAt());
  }
}
