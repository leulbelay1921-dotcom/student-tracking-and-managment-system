package com.sitms.organization;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload for creating or updating an {@link Organization}. */
public record OrganizationRequest(
    @NotBlank @Size(max = 128) String name,
    @Size(max = 128) String industry,
    @Email @Size(max = 255) String contactEmail,
    @Size(max = 32) String contactPhone,
    @Size(max = 255) String address,
    @Size(max = 255) String website,
    @Min(0) Integer positions,
    Boolean active) {

  public boolean activeOrDefault() {
    return active == null || active;
  }
}
