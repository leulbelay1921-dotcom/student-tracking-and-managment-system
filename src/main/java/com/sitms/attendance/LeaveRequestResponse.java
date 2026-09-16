package com.sitms.attendance;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link LeaveRequest}. */
public record LeaveRequestResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    AttendanceRequestStatus status,
    UUID reviewerId,
    String reviewNote,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static LeaveRequestResponse from(LeaveRequest leaveRequest) {
    return new LeaveRequestResponse(
        leaveRequest.getId(),
        leaveRequest.getStudentId(),
        leaveRequest.getInternshipId(),
        leaveRequest.getStartDate(),
        leaveRequest.getEndDate(),
        leaveRequest.getReason(),
        leaveRequest.getStatus(),
        leaveRequest.getReviewerId(),
        leaveRequest.getReviewNote(),
        leaveRequest.getCreatedAt(),
        leaveRequest.getUpdatedAt());
  }
}