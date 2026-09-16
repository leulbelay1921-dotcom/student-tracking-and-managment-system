package com.sitms.student;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link Student}. */
public record StudentResponse(
    UUID id,
    UUID appUserId,
    String appUserUsername,
    String studentNo,
    String fullName,
    String program,
    String department,
    String university,
    String phone,
    String email,
    String status,
    int level,
    Integer batch,
    String profilePhoto,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static StudentResponse from(Student student) {
    return new StudentResponse(
        student.getId(),
        student.getAppUserId(),
        student.getAppUserUsername(),
        student.getStudentNo(),
        student.getFullName(),
        student.getProgram(),
        student.getDepartment(),
        student.getUniversity(),
        student.getPhone(),
        student.getEmail(),
        student.getStatus().name(),
        student.getLevel(),
        student.getBatch(),
        student.getProfilePhoto(),
        student.getCreatedAt(),
        student.getUpdatedAt());
  }

  /** List view: omits the (potentially large) profile photo data URL. */
  static StudentResponse fromWithoutPhoto(Student student) {
    StudentResponse full = from(student);
    return new StudentResponse(
        full.id(),
        full.appUserId(),
        full.appUserUsername(),
        full.studentNo(),
        full.fullName(),
        full.program(),
        full.department(),
        full.university(),
        full.phone(),
        full.email(),
        full.status(),
        full.level(),
        full.batch(),
        null,
        full.createdAt(),
        full.updatedAt());
  }
}