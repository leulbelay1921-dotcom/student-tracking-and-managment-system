package com.sitms.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating a {@link Student} (admin-managed profile). */
public record StudentRequest(
    @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,32}$", message = "App user id must be 3-32 letters, digits, dots, dashes or underscores")
        String appUserUsername,
    @NotBlank @Size(max = 32) String studentNo,
    @NotBlank @Size(max = 128) String fullName,
    @NotBlank @Size(max = 128) String program,
    @Size(max = 128) String department,
    @Size(max = 128) String university,
    String status,
    @Min(1) Integer level,
    @Min(1900) Integer batch,
    @Size(max = 32) String phone,
    @Email @Size(max = 255) String email) {

  public int levelOrDefault() {
    return level == null ? 1 : level;
  }

  public Integer batchOrDefault() {
    return batch != null ? batch : java.time.Year.now().getValue();
  }

  public StudentStatus statusOrDefault() {
    return StudentStatus.fromOrDefault(status, StudentStatus.PENDING);
  }
}