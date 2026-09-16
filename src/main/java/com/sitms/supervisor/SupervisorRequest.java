package com.sitms.supervisor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Payload for creating or updating a {@link Supervisor}. */
public record SupervisorRequest(
    @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9._-]{3,32}$", message = "App user id must be 3-32 letters, digits, dots, dashes or underscores")
        String appUserUsername,
    @NotNull UUID organizationId,
    @NotBlank @Size(max = 32) String employeeNo,
    @NotBlank @Size(max = 128) String fullName,
    @Size(max = 128) String jobTitle,
    @Size(max = 128) String department,
    @Email @Size(max = 255) String email,
    @Size(max = 32) String phone,
    Boolean active) {

  public boolean activeOrDefault() {
    return active == null || active;
  }
}