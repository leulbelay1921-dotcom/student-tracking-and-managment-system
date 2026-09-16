package com.sitms.activity;

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
 * A daily or weekly activity logged by a student during their internship (SITMS-027).
 *
 * <p>{@code studentId} and (nullable) {@code internshipId} are plain UUID references. Lifecycle
 * {@code status} tracks the review workflow via {@link ActivityStatus}. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention. An optional 1:1 {@code ActivityReview}
 * holds the reviewer decision.
 */
@Entity
@Table(name = "activity")
@EntityListeners(AuditingEntityListener.class)
public class Activity {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id")
  private UUID internshipId;

  @Column(name = "activity_date", nullable = false)
  private LocalDate activityDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 16)
  private ActivityType type = ActivityType.DAILY;

  @Column(name = "title", length = 128)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Column(name = "skills", columnDefinition = "TEXT")
  private String skills;

  @Column(name = "hours")
  private Double hours;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ActivityStatus status = ActivityStatus.DRAFT;

  @Column(name = "academic_reviewer_id")
  private UUID academicReviewerId;

  @Column(name = "academic_feedback", columnDefinition = "TEXT")
  private String academicFeedback;

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

  protected Activity() {}

  private Activity(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.activityDate = builder.activityDate;
    this.type = builder.type;
    this.title = builder.title;
    this.description = builder.description;
    this.skills = builder.skills;
    this.hours = builder.hours;
    this.status = builder.status;
    this.academicReviewerId = builder.academicReviewerId;
    this.academicFeedback = builder.academicFeedback;
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

  public LocalDate getActivityDate() {
    return activityDate;
  }

  public ActivityType getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public String getSkills() {
    return skills;
  }

  public Double getHours() {
    return hours;
  }

  public ActivityStatus getStatus() {
    return status;
  }

  public UUID getAcademicReviewerId() {
    return academicReviewerId;
  }

  public String getAcademicFeedback() {
    return academicFeedback;
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
      UUID internshipId,
      LocalDate activityDate,
      ActivityType type,
      String title,
      String description,
      String skills,
      Double hours) {
    this.internshipId = internshipId;
    this.activityDate = activityDate;
    this.type = type;
    this.title = title;
    this.description = description;
    this.skills = skills;
    this.hours = hours;
  }

  void changeStatus(ActivityStatus status) {
    this.status = status;
  }

  void applyAcademicReview(UUID reviewerId, String feedback) {
    this.academicReviewerId = reviewerId;
    this.academicFeedback = feedback;
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private LocalDate activityDate;
    private ActivityType type = ActivityType.DAILY;
    private String title;
    private String description;
    private String skills;
    private Double hours;
    private ActivityStatus status = ActivityStatus.DRAFT;
    private UUID academicReviewerId;
    private String academicFeedback;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
      return this;
    }

    public Builder activityDate(LocalDate activityDate) {
      this.activityDate = activityDate;
      return this;
    }

    public Builder type(ActivityType type) {
      this.type = type;
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

    public Builder skills(String skills) {
      this.skills = skills;
      return this;
    }

    public Builder hours(Double hours) {
      this.hours = hours;
      return this;
    }

    public Builder status(ActivityStatus status) {
      this.status = status;
      return this;
    }

    public Builder academicReviewerId(UUID academicReviewerId) {
      this.academicReviewerId = academicReviewerId;
      return this;
    }

    public Builder academicFeedback(String academicFeedback) {
      this.academicFeedback = academicFeedback;
      return this;
    }

    public Activity build() {
      return new Activity(this);
    }
  }
}
