package com.sitms.internship;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A student's internship placement (SITMS-018).
 *
 * <p>{@code studentId}, {@code organizationId}, (nullable) {@code supervisorId} and (nullable)
 * {@code academicStaffId} are plain UUID references to their respective tables: the supervisor is
 * the company-side workplace supervisor, the academic staff is the university-side academic
 * supervisor (SITMS-072). The lifecycle {@code status} is tracked via {@link InternshipStatus}.
 * Audit columns and the optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "internship")
@EntityListeners(AuditingEntityListener.class)
public class Internship {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "organization_id", nullable = false)
  private UUID organizationId;

  @Column(name = "supervisor_id")
  private UUID supervisorId;

  @Column(name = "academic_staff_id")
  private UUID academicStaffId;

  @Column(name = "title", nullable = false, length = 128)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "end_date")
  private LocalDate endDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private InternshipStatus status = InternshipStatus.PENDING;

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

  protected Internship() {}

  private Internship(Builder builder) {
    this.studentId = builder.studentId;
    this.organizationId = builder.organizationId;
    this.supervisorId = builder.supervisorId;
    this.academicStaffId = builder.academicStaffId;
    this.title = builder.title;
    this.description = builder.description;
    this.startDate = builder.startDate;
    this.endDate = builder.endDate;
    this.status = builder.status;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public UUID getOrganizationId() {
    return organizationId;
  }

  public UUID getSupervisorId() {
    return supervisorId;
  }

  public UUID getAcademicStaffId() {
    return academicStaffId;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public InternshipStatus getStatus() {
    return status;
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
      UUID supervisorId,
      UUID academicStaffId,
      String title,
      String description,
      LocalDate startDate,
      LocalDate endDate) {
    this.organizationId = organizationId;
    this.supervisorId = supervisorId;
    this.academicStaffId = academicStaffId;
    this.title = title;
    this.description = description;
    this.startDate = startDate;
    this.endDate = endDate;
  }

  void assignSupervisor(UUID supervisorId) {
    this.supervisorId = supervisorId;
  }

  void assignAcademicStaff(UUID academicStaffId) {
    this.academicStaffId = academicStaffId;
  }

  void changeStatus(InternshipStatus status) {
    this.status = status;
  }

  public static final class Builder {
    private UUID studentId;
    private UUID organizationId;
    private UUID supervisorId;
    private UUID academicStaffId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate endDate;
    private InternshipStatus status = InternshipStatus.PENDING;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder organizationId(UUID organizationId) {
      this.organizationId = organizationId;
      return this;
    }

    public Builder supervisorId(UUID supervisorId) {
      this.supervisorId = supervisorId;
      return this;
    }

    public Builder academicStaffId(UUID academicStaffId) {
      this.academicStaffId = academicStaffId;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder description(String description) {
      this.description = description;
      return this;
    }

    public Builder startDate(LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    public Builder endDate(LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    public Builder status(InternshipStatus status) {
      this.status = status;
      return this;
    }

    public Internship build() {
      return new Internship(this);
    }
  }
}
