package com.sitms.academicstaff;

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
 * University-side academic supervisor (SITMS-072).
 *
 * <p>{@code app_user_id} is a 1:1 reference to {@code app_user.id}; unlike a workplace
 * {@code supervisor} this profile is not tied to a hosting organization. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "academic_staff")
@EntityListeners(AuditingEntityListener.class)
public class AcademicStaff {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "app_user_id", nullable = false, unique = true)
  private UUID appUserId;

  /** The short login-style id (e.g. {@code fac001}) backing this member's {@code app_user}. */
  @Column(name = "username", nullable = false, unique = true, length = 64)
  private String appUserUsername;

  @Column(name = "staff_no", nullable = false, unique = true, length = 32)
  private String staffNo;

  @Column(name = "full_name", nullable = false, length = 128)
  private String fullName;

  @Column(name = "title", length = 128)
  private String title;

  @Column(name = "department", length = 128)
  private String department;

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

  protected AcademicStaff() {}

  private AcademicStaff(Builder builder) {
    this.appUserId = builder.appUserId;
    this.appUserUsername = builder.appUserUsername;
    this.staffNo = builder.staffNo;
    this.fullName = builder.fullName;
    this.title = builder.title;
    this.department = builder.department;
    this.active = builder.active;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public UUID getAppUserId() {
    return appUserId;
  }

  public String getAppUserUsername() {
    return appUserUsername;
  }

  public String getStaffNo() {
    return staffNo;
  }

  public String getFullName() {
    return fullName;
  }

  public String getTitle() {
    return title;
  }

  public String getDepartment() {
    return department;
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

  void renameTo(String appUserUsername) {
    this.appUserUsername = appUserUsername;
  }

  void update(
      String staffNo,
      String fullName,
      String title,
      String department,
      boolean active) {
    this.staffNo = staffNo;
    this.fullName = fullName;
    this.title = title;
    this.department = department;
    this.active = active;
  }

  public static final class Builder {
    private UUID appUserId;
    private String appUserUsername;
    private String staffNo;
    private String fullName;
    private String title;
    private String department;
    private boolean active = true;

    private Builder() {}

    public Builder appUserId(UUID appUserId) {
      this.appUserId = appUserId;
      return this;
    }

    public Builder appUserUsername(String appUserUsername) {
      this.appUserUsername = appUserUsername;
      return this;
    }

    public Builder staffNo(String staffNo) {
      this.staffNo = staffNo;
      return this;
    }

    public Builder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder department(String department) {
      this.department = department;
      return this;
    }

    public Builder active(boolean active) {
      this.active = active;
      return this;
    }

    public AcademicStaff build() {
      return new AcademicStaff(this);
    }
  }
}