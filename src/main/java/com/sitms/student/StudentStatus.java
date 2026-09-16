package com.sitms.student;

/** Internship-track status of a {@link Student} (SITMS-007). */
public enum StudentStatus {
  ACTIVE,
  PENDING,
  COMPLETED;

  public static StudentStatus fromOrDefault(String value, StudentStatus fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }
    for (StudentStatus status : values()) {
      if (status.name().equalsIgnoreCase(value.trim())) {
        return status;
      }
    }
    return fallback;
  }
}
