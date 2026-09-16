package com.sitms.supervisor;

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
 * Internship workplace supervisor (SITMS-014).
 *
 * <p>{@code app_user_id} is a 1:1 reference to {@code app_user.id}; {@code organization_id}
 * references the hosting {@code organization}. Audit columns and the optimistic-lock
 * {@code version} are populated by Spring Data JPA auditing (see {@code JpaAuditingConfig}).
 */
@Entity
@Table(name = "supervisor")
@EntityListeners(AuditingEntityListener.class)
public class Supervisor {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "app_user_id", nullable = false, unique = true)
  private UUID appUserId;

  /** The short login-style id (e.g. {@code sup001}) backing this supervisor's {@code app_user}. */
  @Column(name = "username", nullable = false, unique = true, length = 64)
  private String appUserUsername;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "employee_no", nullable = false, unique = true, length = 32)
  private String employeeNo;

  @Column(name = "full_name", nullable = false, length = 128)
  private String fullName;

  @Column(name = "job_title", length = 128)
  private String jobTitle;

  @Column(name = "department", length = 128)
  private String department;

  @Column(name = "email", length = 255)
  private String email;

  @Column(name = "phone", length = 32)
  private String phone;

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

  protected Supervisor() {}

  private Supervisor(Builder builder) {
    this.appUserId = builder.appUserId;
    this.appUserUsername = builder.appUserUsername;
    this.organizationId = builder.organizationId;
    this.employeeNo = builder.employeeNo;
    this.fullName = builder.fullName;
    this.jobTitle = builder.jobTitle;
    this.department = builder.department;
    this.email = builder.email;
    this.phone = builder.phone;
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

  public UUID getOrganizationId() {
    return organizationId;
  }

  public String getEmployeeNo() {
    return employeeNo;
  }

  public String getFullName() {
    return fullName;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public String getDepartment() {
    return department;
  }

  public boolean isActive() {
    return active;
  }

  public String getEmail() {
    return email;
  }

  public String getPhone() {
    return phone;
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
      UUID organizationId,
      String employeeNo,
      String fullName,
      String jobTitle,
      String department,
      String email,
      String phone,
      boolean active) {
    this.organizationId = organizationId;
    this.employeeNo = employeeNo;
    this.fullName = fullName;
    this.jobTitle = jobTitle;
    this.department = department;
    this.email = email;
    this.phone = phone;
    this.active = active;
  }

  void renameTo(String username) {
    this.appUserUsername = username;
  }

  public static final class Builder {
    private UUID appUserId;
    private String appUserUsername;
    private UUID organizationId;
    private String employeeNo;
    private String fullName;
    private String jobTitle;
    private String department;
    private String email;
    private String phone;
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

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder employeeNo(String employeeNo) {
      this.employeeNo = employeeNo;
      return this;
    }

    public Builder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public Builder jobTitle(String jobTitle) {
      this.jobTitle = jobTitle;
      return this;
    }

    public Builder department(String department) {
      this.department = department;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder phone(String phone) {
      this.phone = phone;
      return this;
    }

    public Builder active(boolean active) {
      this.active = active;
      return this;
    }

    public Supervisor build() {
      return new Supervisor(this);
    }
  }
}
