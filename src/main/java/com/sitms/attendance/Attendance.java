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
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A single day of internship attendance for a student (SITMS-070).
 *
 * <p>{@code studentId} and (nullable) {@code internshipId} are plain UUID references. {@code
 * status} is PRESENT or ABSENT as recorded by the supervising intern. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "attendance")
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id")
  private UUID internshipId;

  @Column(name = "attendance_date", nullable = false)
  private LocalDate attendanceDate;

  @Column(name = "check_in")
  private LocalTime checkIn;

  @Column(name = "check_out")
  private LocalTime checkOut;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private AttendanceStatus status = AttendanceStatus.PRESENT;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

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

  protected Attendance() {}

  private Attendance(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.attendanceDate = builder.attendanceDate;
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

  public LocalDate getAttendanceDate() {
    return attendanceDate;
  }

  public LocalTime getCheckIn() {
    return checkIn;
  }

  public LocalTime getCheckOut() {
    return checkOut;
  }

  public AttendanceStatus getStatus() {
    return status;
  }

  public String getNote() {
    return note;
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

  void record(AttendanceStatus status, String note) {
    this.checkIn = null;
    this.checkOut = null;
    this.note = note;
    this.status = status;
  }

  void checkIn(LocalTime time, AttendanceStatus status) {
    this.checkIn = time;
    this.checkOut = null;
    this.note = status == AttendanceStatus.LATE ? "Checked in late." : null;
    this.status = status;
  }

  void checkOut(LocalTime time) {
    this.checkOut = time;
  }

  /** Applies a supervisor correction; provided values overwrite the row, absent ones are kept. */
  void correct(AttendanceStatus status, LocalTime checkIn, LocalTime checkOut, String note) {
    if (status != null) {
      this.status = status;
    }
    if (checkIn != null) {
      this.checkIn = checkIn;
    }
    if (checkOut != null) {
      this.checkOut = checkOut;
    }
    if (note != null) {
      this.note = note;
    }
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private LocalDate attendanceDate;
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
      return this;
    }

    public Builder attendanceDate(LocalDate attendanceDate) {
      this.attendanceDate = attendanceDate;
      return this;
    }

    public Builder status(AttendanceStatus status) {
      this.status = status;
      return this;
    }

    public Attendance build() {
      return new Attendance(this);
    }
  }
}