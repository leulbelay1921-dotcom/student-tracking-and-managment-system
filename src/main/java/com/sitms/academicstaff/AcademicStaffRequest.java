package com.sitms.academicstaff;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating an {@link AcademicStaff}. */
public record AcademicStaffRequest(
    @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,32}$", message = "App user id must be 3-32 letters, digits, dots, dashes or underscores")
        String appUserUsername,
    @NotBlank @Size(max = 32) String staffNo,
    @NotBlank @Size(max = 128) String fullName,
    @Size(max = 128) String title,
    @Size(max = 128) String department,
    Boolean active) {

  public boolean activeOrDefault() {
    return active == null || active;
  }
}