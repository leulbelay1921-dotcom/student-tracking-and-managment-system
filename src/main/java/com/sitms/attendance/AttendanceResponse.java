package com.sitms.attendance;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link Attendance} record. */
public record AttendanceResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    LocalDate attendanceDate,
    LocalTime checkIn,
    LocalTime checkOut,
    AttendanceStatus status,
    String note,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static AttendanceResponse from(Attendance attendance) {
    return new AttendanceResponse(
        attendance.getId(),
        attendance.getStudentId(),
        attendance.getInternshipId(),
        attendance.getAttendanceDate(),
        attendance.getCheckIn(),
        attendance.getCheckOut(),
        attendance.getStatus(),
        attendance.getNote(),
        attendance.getCreatedAt(),
        attendance.getUpdatedAt());
  }
}