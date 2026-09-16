package com.sitms.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Employer/internship host organization (SITMS-011).
 *
 * <p>Audit columns and the optimistic-lock {@code version} are populated by Spring Data JPA
 * auditing (see {@code JpaAuditingConfig}) on persist/update, matching the V1 convention.
 */
@Entity
@Table(name = "organization")
@EntityListeners(AuditingEntityListener.class)
public class Organization {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "name", nullable = false, unique = true, length = 128)
  private String name;

  @Column(name = "industry", length = 128)
  private String industry;

  @Column(name = "contact_email", length = 255)
  private String contactEmail;

  @Column(name = "contact_phone", length = 32)
  private String contactPhone;

  @Column(name = "address", length = 255)
  private String address;

  @Column(name = "website", length = 255)
  private String website;

  @Column(name = "positions")
  private Integer positions;

  @Column(name = "active", nullable = false)
  private boolean active = true;

  @Version
  @Column(name = "version", nullable = false)
  private long version;

  @CreatedBy
  @Column(name = "created_by", nullable = false)
  private UUID createdBy;

  @LastModifiedBy
  @Column(name = "updated_by")
  private UUID updatedBy;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt;

  protected Organization() {}

  private Organization(Builder builder) {
    this.name = builder.name;
    this.industry = builder.industry;
    this.contactEmail = builder.contactEmail;
    this.contactPhone = builder.contactPhone;
    this.address = builder.address;
    this.website = builder.website;
    this.positions = builder.positions;
    this.active = builder.active;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getIndustry() {
    return industry;
  }

  public String getContactEmail() {
    return contactEmail;
  }

  public String getContactPhone() {
    return contactPhone;
  }

  public String getAddress() {
    return address;
  }

  public String getWebsite() {
    return website;
  }

  public Integer getPositions() {
    return positions;
  }

  public boolean isActive() {
    return active;
  }

  public long getVersion() {
    return version;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  void update(
      String name,
      String industry,
      String contactEmail,
      String contactPhone,
      String address,
      String website,
      Integer positions,
      boolean active) {
    this.name = name;
    this.industry = industry;
    this.contactEmail = contactEmail;
    this.contactPhone = contactPhone;
    this.address = address;
    this.website = website;
    this.positions = positions;
    this.active = active;
  }

  public static final class Builder {
    private String name;
    private String industry;
    private String contactEmail;
    private String contactPhone;
    private String address;
    private String website;
    private Integer positions;
    private boolean active = true;

    private Builder() {}

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder industry(String industry) {
      this.industry = industry;
      return this;
    }

    public Builder contactEmail(String contactEmail) {
      this.contactEmail = contactEmail;
      return this;
    }

    public Builder contactPhone(String contactPhone) {
      this.contactPhone = contactPhone;
      return this;
    }

    public Builder address(String address) {
      this.address = address;
      return this;
    }

    public Builder website(String website) {
      this.website = website;
      return this;
    }

    public Builder positions(Integer positions) {
      this.positions = positions;
      return this;
    }

    public Builder active(boolean active) {
      this.active = active;
      return this;
    }

    public Organization build() {
      return new Organization(this);
    }
  }
}
