package com.sitms.internship;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link Internship}. */
public record InternshipResponse(
    UUID id,
    UUID studentId,
    UUID organizationId,
    UUID supervisorId,
    UUID academicStaffId,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    InternshipStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static InternshipResponse from(Internship internship) {
    return new InternshipResponse(
        internship.getId(),
        internship.getStudentId(),
        internship.getOrganizationId(),
        internship.getSupervisorId(),
        internship.getAcademicStaffId(),
        internship.getTitle(),
        internship.getDescription(),
        internship.getStartDate(),
        internship.getEndDate(),
        internship.getStatus(),
        internship.getCreatedAt(),
        internship.getUpdatedAt());
  }
}
