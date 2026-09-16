package com.sitms.attendance;

/** Review state shared by {@link LeaveRequest} and {@link AttendanceIssue}. */
public enum AttendanceRequestStatus {
  PENDING,
  APPROVED,
  REJECTED
}