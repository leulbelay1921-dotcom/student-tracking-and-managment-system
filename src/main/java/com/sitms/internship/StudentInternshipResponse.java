package com.sitms.internship;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Read-only view of a student's own internship assignment, with referenced names resolved. */
public record StudentInternshipResponse(
    UUID id,
    UUID studentId,
    UUID organizationId,
    UUID supervisorId,
    UUID academicStaffId,
    String organizationName,
    String supervisorName,
    String academicStaffName,
    String title,
    String description,
    LocalDate startDate,
    LocalDate endDate,
    InternshipStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static StudentInternshipResponse from(
      Internship internship,
      String organizationName,
      String supervisorName,
      String academicStaffName) {
    return new StudentInternshipResponse(
        internship.getId(),
        internship.getStudentId(),
        internship.getOrganizationId(),
        internship.getSupervisorId(),
        internship.getAcademicStaffId(),
        organizationName,
        supervisorName,
        academicStaffName,
        internship.getTitle(),
        internship.getDescription(),
        internship.getStartDate(),
        internship.getEndDate(),
        internship.getStatus(),
        internship.getCreatedAt(),
        internship.getUpdatedAt());
  }
}