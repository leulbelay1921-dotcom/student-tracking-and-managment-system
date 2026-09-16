package com.sitms.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Payload for creating or updating a {@link Task}.
 *
 * <p>{@code supervisorId} is intentionally absent: only a company supervisor may create or edit
 * tasks, and the acting supervisor is always derived from the authenticated principal. The task is
 * assigned to {@code studentId}, who must be one of the supervisor's assigned students.
 */
public record TaskRequest(
    @NotNull UUID studentId,
    UUID internshipId,
    @NotBlank @Size(max = 128) String title,
    @Size(max = 2000) String description,
    LocalDate startDate,
    LocalDate dueDate,
    @Size(max = 64) String category,
    @Size(max = 2000) String expectedOutcome,
    TaskPriority priority) {}