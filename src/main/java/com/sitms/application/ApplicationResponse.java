package com.sitms.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link Application}. */
public record ApplicationResponse(
    UUID id,
    UUID studentId,
    UUID organizationId,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    ApplicationStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static ApplicationResponse from(Application application) {
    return new ApplicationResponse(
        application.getId(),
        application.getStudentId(),
        application.getOrganizationId(),
        application.getTitle(),
        application.getDescription(),
        application.getStartDate(),
        application.getEndDate(),
        application.getStatus(),
        application.getCreatedAt(),
        application.getUpdatedAt());
  }
}