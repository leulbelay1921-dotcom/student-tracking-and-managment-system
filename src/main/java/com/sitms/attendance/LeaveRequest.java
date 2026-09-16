package com.sitms.attendance;

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
 * A student's leave request covering one or more internship days (SITMS-070). A supervisor (or an
 * admin on their behalf) approves or rejects it via {@code reviewerId}/{@code reviewNote}.
 *
 * <p>{@code studentId} and (nullable) {@code internshipId} are plain UUID references. Audit columns
 * and the optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "leave_request")
@EntityListeners(AuditingEntityListener.class)
public class LeaveRequest {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id")
  private UUID internshipId;

  @Column(name = "start_date", nullable = false)
  private LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  private LocalDate endDate;

  @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
  private String reason;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private AttendanceRequestStatus status = AttendanceRequestStatus.PENDING;

  @Column(name = "reviewer_id")
  private UUID reviewerId;

  @Column(name = "review_note", columnDefinition = "TEXT")
  private String reviewNote;

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

  protected LeaveRequest() {}

  private LeaveRequest(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.startDate = builder.startDate;
    this.endDate = builder.endDate;
    this.reason = builder.reason;
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

  public UUID getInternshipId() {
    return internshipId;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public String getReason() {
    return reason;
  }

  public AttendanceRequestStatus getStatus() {
    return status;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public String getReviewNote() {
    return reviewNote;
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

  void updateReview(UUID reviewerId, AttendanceRequestStatus status, String reviewNote) {
    this.reviewerId = reviewerId;
    this.status = status;
    this.reviewNote = reviewNote;
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private AttendanceRequestStatus status = AttendanceRequestStatus.PENDING;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
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

    public Builder reason(String reason) {
      this.reason = reason;
      return this;
    }

    public Builder status(AttendanceRequestStatus status) {
      this.status = status;
      return this;
    }

    public LeaveRequest build() {
      return new LeaveRequest(this);
    }
  }
}