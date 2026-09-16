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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A student's report that a recorded attendance row is incorrect (SITMS-070). A supervisor (or an
 * admin on their behalf) responds via {@code reviewerId}/{@code response}, leaving the attendance
 * row itself for manual correction.
 *
 * <p>{@code attendanceId} and {@code studentId} are plain UUID references. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "attendance_issue")
@EntityListeners(AuditingEntityListener.class)
public class AttendanceIssue {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "attendance_id", nullable = false)
  private UUID attendanceId;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "message", nullable = false, columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private AttendanceRequestStatus status = AttendanceRequestStatus.PENDING;

  @Column(name = "reviewer_id")
  private UUID reviewerId;

  @Column(name = "response", columnDefinition = "TEXT")
  private String response;

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

  protected AttendanceIssue() {}

  private AttendanceIssue(Builder builder) {
    this.attendanceId = builder.attendanceId;
    this.studentId = builder.studentId;
    this.message = builder.message;
    this.status = builder.status;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public UUID getAttendanceId() {
    return attendanceId;
  }

  public UUID getStudentId() {
    return studentId;
  }

  public String getMessage() {
    return message;
  }

  public AttendanceRequestStatus getStatus() {
    return status;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public String getResponse() {
    return response;
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

  void updateReview(UUID reviewerId, AttendanceRequestStatus status, String response) {
    this.reviewerId = reviewerId;
    this.status = status;
    this.response = response;
  }

  public static final class Builder {
    private UUID attendanceId;
    private UUID studentId;
    private String message;
    private AttendanceRequestStatus status = AttendanceRequestStatus.PENDING;

    private Builder() {}

    public Builder attendanceId(UUID attendanceId) {
      this.attendanceId = attendanceId;
      return this;
    }

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    public Builder status(AttendanceRequestStatus status) {
      this.status = status;
      return this;
    }

    public AttendanceIssue build() {
      return new AttendanceIssue(this);
    }
  }
}