package com.sitms.organization;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link Organization}. */
public record OrganizationResponse(
    UUID id,
    String name,
    String industry,
    String contactEmail,
    String contactPhone,
    String address,
    String website,
    Integer positions,
    boolean active,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static OrganizationResponse from(Organization organization) {
    return new OrganizationResponse(
        organization.getId(),
        organization.getName(),
        organization.getIndustry(),
        organization.getContactEmail(),
        organization.getContactPhone(),
        organization.getAddress(),
        organization.getWebsite(),
        organization.getPositions(),
        organization.isActive(),
        organization.getCreatedAt(),
        organization.getUpdatedAt());
  }
}
