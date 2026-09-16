package com.sitms.academicstaff;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link AcademicStaff}. */
public record AcademicStaffResponse(
    UUID id,
    UUID appUserId,
    String appUserUsername,
    String staffNo,
    String fullName,
    String title,
    String department,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static AcademicStaffResponse from(AcademicStaff staff) {
    return new AcademicStaffResponse(
        staff.getId(),
        staff.getAppUserId(),
        staff.getAppUserUsername(),
        staff.getStaffNo(),
        staff.getFullName(),
        staff.getTitle(),
        staff.getDepartment(),
        staff.isActive(),
        staff.getCreatedAt(),
        staff.getUpdatedAt());
  }
}