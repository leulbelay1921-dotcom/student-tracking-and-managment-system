package com.sitms.supervisor;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link Supervisor}. */
public record SupervisorResponse(
    UUID id,
    UUID appUserId,
    String appUserUsername,
    UUID organizationId,
    String employeeNo,
    String fullName,
    String jobTitle,
    String department,
    String email,
    String phone,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static SupervisorResponse from(Supervisor supervisor) {
    return new SupervisorResponse(
        supervisor.getId(),
        supervisor.getAppUserId(),
        supervisor.getAppUserUsername(),
        supervisor.getOrganizationId(),
        supervisor.getEmployeeNo(),
        supervisor.getFullName(),
        supervisor.getJobTitle(),
        supervisor.getDepartment(),
        supervisor.getEmail(),
        supervisor.getPhone(),
        supervisor.isActive(),
        supervisor.getCreatedAt(),
        supervisor.getUpdatedAt());
  }
}
