package com.sitms.attendance;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link AttendanceIssue}. */
public record AttendanceIssueResponse(
    UUID id,
    UUID attendanceId,
    UUID studentId,
    String message,
    AttendanceRequestStatus status,
    UUID reviewerId,
    String response,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static AttendanceIssueResponse from(AttendanceIssue issue) {
    return new AttendanceIssueResponse(
        issue.getId(),
        issue.getAttendanceId(),
        issue.getStudentId(),
        issue.getMessage(),
        issue.getStatus(),
        issue.getReviewerId(),
        issue.getResponse(),
        issue.getCreatedAt(),
        issue.getUpdatedAt());
  }
}