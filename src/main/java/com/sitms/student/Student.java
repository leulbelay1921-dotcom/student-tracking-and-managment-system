package com.sitms.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Student profile extending the shared {@code app_user} identity table (SITMS-006).
 *
 * <p>{@code app_user_id} is a 1:1 reference to {@code app_user.id}. Audit columns and the
 * optimistic-lock {@code version} are populated/reconciled against the DB defaults by Spring Data
 * JPA auditing (see {@code JpaAuditingConfig}) on persist/update.
 */
@Entity
@Table(name = "student")
@EntityListeners(AuditingEntityListener.class)
public class Student {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "app_user_id", nullable = false, unique = true)
  private UUID appUserId;

  /** The short login-style id (e.g. {@code fk001}) chosen by the admin when creating the student. */
  @Column(name = "username", nullable = false, unique = true, length = 64)
  private String appUserUsername;

  @Column(name = "student_no", nullable = false, unique = true, length = 32)
  private String studentNo;

  @Column(name = "full_name", nullable = false, length = 128)
  private String fullName;

  @Column(name = "program", nullable = false, length = 128)
  private String program;

  @Column(name = "department", length = 128)
  private String department;

  @Column(name = "university", length = 128)
  private String university;

  @Column(name = "phone", length = 32)
  private String phone;

  @Column(name = "email", length = 255)
  private String email;

  /** Profile photo as a {@code data:image/...;base64,...} URL set by the student or an admin. */
  @Column(name = "profile_photo", columnDefinition = "TEXT")
  private String profilePhoto;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private StudentStatus status = StudentStatus.PENDING;

  @Column(name = "level", nullable = false)
  private int level = 1;

  /** Admission intake year, e.g. 2026 for students who started their programme that year. */
  @Column(name = "batch")
  private Integer batch;

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

  protected Student() {}

  private Student(Builder builder) {
    this.appUserId = builder.appUserId;
    this.appUserUsername = builder.appUserUsername;
    this.studentNo = builder.studentNo;
    this.fullName = builder.fullName;
    this.program = builder.program;
    this.department = builder.department;
    this.university = builder.university;
    this.phone = builder.phone;
    this.email = builder.email;
    this.profilePhoto = builder.profilePhoto;
    this.status = builder.status;
    this.level = builder.level;
    this.batch = builder.batch;
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

  public String getStudentNo() {
    return studentNo;
  }

  public String getFullName() {
    return fullName;
  }

  public String getProgram() {
    return program;
  }

  public String getDepartment() {
    return department;
  }

  public String getUniversity() {
    return university;
  }

  public String getPhone() {
    return phone;
  }

  public String getEmail() {
    return email;
  }

  public String getProfilePhoto() {
    return profilePhoto;
  }

  public StudentStatus getStatus() {
    return status;
  }

  public int getLevel() {
    return level;
  }

  public Integer getBatch() {
    return batch;
  }

  public long getVersion() {
    return version;
  }

  void updateProfile(
      String studentNo,
      String fullName,
      String program,
      String department,
      String university,
      StudentStatus status,
      int level,
      Integer batch,
      String phone,
      String email) {
    this.studentNo = studentNo;
    this.fullName = fullName;
    this.program = program;
    this.department = department;
    this.university = university;
    this.status = status;
    this.level = level;
    this.batch = batch;
    this.phone = phone;
    this.email = email;
  }

  /** Student self-service: only the contact fields are writable by the owner. */
  void updateContact(String email, String phone) {
    this.email = email;
    this.phone = phone;
  }

  void updateProfilePhoto(String profilePhoto) {
    this.profilePhoto = profilePhoto;
  }

  void renameTo(String username) {
    this.appUserUsername = username;
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

  public static final class Builder {
    private UUID appUserId;
    private String appUserUsername;
    private String studentNo;
    private String fullName;
    private String program;
    private String department;
    private String university;
    private String phone;
    private String email;
    private String profilePhoto;
    private StudentStatus status = StudentStatus.PENDING;
    private int level = 1;
    private Integer batch;

    private Builder() {}

    public Builder appUserId(UUID appUserId) {
      this.appUserId = appUserId;
      return this;
    }

    public Builder appUserUsername(String appUserUsername) {
      this.appUserUsername = appUserUsername;
      return this;
    }

    public Builder studentNo(String studentNo) {
      this.studentNo = studentNo;
      return this;
    }

    public Builder fullName(String fullName) {
      this.fullName = fullName;
      return this;
    }

    public Builder program(String program) {
      this.program = program;
      return this;
    }

    public Builder department(String department) {
      this.department = department;
      return this;
    }

    public Builder university(String university) {
      this.university = university;
      return this;
    }

    public Builder phone(String phone) {
      this.phone = phone;
      return this;
    }

    public Builder email(String email) {
      this.email = email;
      return this;
    }

    public Builder profilePhoto(String profilePhoto) {
      this.profilePhoto = profilePhoto;
      return this;
    }

    public Builder status(StudentStatus status) {
      this.status = status;
      return this;
    }

    public Builder level(int level) {
      this.level = level;
      return this;
    }

    public Builder batch(Integer batch) {
      this.batch = batch;
      return this;
    }

    public Student build() {
      return new Student(this);
    }
  }
}
