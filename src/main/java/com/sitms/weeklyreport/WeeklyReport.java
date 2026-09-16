package com.sitms.weeklyreport;

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
 * A student's weekly report for an internship week, reviewed in two stages: the company supervisor
 * decides first, then the academic staff gives the final verdict (SITMS-077).
 *
 * <p>{@code studentId} and (nullable) {@code internshipId} are plain UUID references. Lifecycle
 * {@code status} tracks the two-stage review workflow via {@link WeeklyReportStatus}. Audit columns
 * and the optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "weekly_report")
@EntityListeners(AuditingEntityListener.class)
public class WeeklyReport {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id")
  private UUID internshipId;

  @Column(name = "week_number", nullable = false)
  private Integer weekNumber;

  @Column(name = "title", length = 128)
  private String title;

  @Column(name = "content", nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "activities_completed", columnDefinition = "TEXT")
  private String activitiesCompleted;

  @Column(name = "problems_encountered", columnDefinition = "TEXT")
  private String problemsEncountered;

  @Column(name = "skills_learned", columnDefinition = "TEXT")
  private String skillsLearned;

  @Column(name = "next_week_plan", columnDefinition = "TEXT")
  private String nextWeekPlan;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 24)
  private WeeklyReportStatus status;

  @Column(name = "company_reviewer_id")
  private UUID companyReviewerId;

  @Column(name = "company_review_note", columnDefinition = "TEXT")
  private String companyReviewNote;

  @Column(name = "academic_reviewer_id")
  private UUID academicReviewerId;

  @Column(name = "academic_review_note", columnDefinition = "TEXT")
  private String academicReviewNote;

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

  protected WeeklyReport() {}

  private WeeklyReport(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.weekNumber = builder.weekNumber;
    this.title = builder.title;
    this.content = builder.content;
    this.activitiesCompleted = builder.activitiesCompleted;
    this.problemsEncountered = builder.problemsEncountered;
    this.skillsLearned = builder.skillsLearned;
    this.nextWeekPlan = builder.nextWeekPlan;
    this.status = builder.status;
    this.companyReviewerId = builder.companyReviewerId;
    this.companyReviewNote = builder.companyReviewNote;
    this.academicReviewerId = builder.academicReviewerId;
    this.academicReviewNote = builder.academicReviewNote;
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

  public Integer getWeekNumber() {
    return weekNumber;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public String getActivitiesCompleted() {
    return activitiesCompleted;
  }

  public String getProblemsEncountered() {
    return problemsEncountered;
  }

  public String getSkillsLearned() {
    return skillsLearned;
  }

  public String getNextWeekPlan() {
    return nextWeekPlan;
  }

  public WeeklyReportStatus getStatus() {
    return status;
  }

  public UUID getCompanyReviewerId() {
    return companyReviewerId;
  }

  public String getCompanyReviewNote() {
    return companyReviewNote;
  }

  public UUID getAcademicReviewerId() {
    return academicReviewerId;
  }

  public String getAcademicReviewNote() {
    return academicReviewNote;
  }

  public long getVersion() {
    return version;
  }

  public OffsetDateTime getCreatedAt() {
    return createdAt;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  void update(
      String title,
      String content,
      Integer weekNumber,
      UUID internshipId,
      String activitiesCompleted,
      String problemsEncountered,
      String skillsLearned,
      String nextWeekPlan) {
    this.title = title;
    this.content = content;
    this.weekNumber = weekNumber;
    this.internshipId = internshipId;
    this.activitiesCompleted = activitiesCompleted;
    this.problemsEncountered = problemsEncountered;
    this.skillsLearned = skillsLearned;
    this.nextWeekPlan = nextWeekPlan;
    this.status = WeeklyReportStatus.PENDING_COMPANY;
    this.companyReviewerId = null;
    this.companyReviewNote = null;
    this.academicReviewerId = null;
    this.academicReviewNote = null;
  }

  void applyCompanyReview(UUID reviewerId, boolean approved, String note) {
    this.companyReviewerId = reviewerId;
    this.companyReviewNote = note;
    this.status =
        approved ? WeeklyReportStatus.COMPANY_APPROVED : WeeklyReportStatus.REJECTED;
  }

  void applyAcademicReview(UUID reviewerId, boolean approved, String note) {
    this.academicReviewerId = reviewerId;
    this.academicReviewNote = note;
    this.status = approved ? WeeklyReportStatus.APPROVED : WeeklyReportStatus.REJECTED;
  }

  /** The academic staff sends the report back for corrections without rejecting it. */
  void applyCorrectionRequest(UUID reviewerId, String note) {
    this.academicReviewerId = reviewerId;
    this.academicReviewNote = note;
    this.status = WeeklyReportStatus.CORRECTION_REQUESTED;
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private Integer weekNumber;
    private String title;
    private String content;
    private String activitiesCompleted;
    private String problemsEncountered;
    private String skillsLearned;
    private String nextWeekPlan;
    private WeeklyReportStatus status;
    private UUID companyReviewerId;
    private String companyReviewNote;
    private UUID academicReviewerId;
    private String academicReviewNote;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
      return this;
    }

    public Builder weekNumber(Integer weekNumber) {
      this.weekNumber = weekNumber;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder content(String content) {
      this.content = content;
      return this;
    }

    public Builder activitiesCompleted(String activitiesCompleted) {
      this.activitiesCompleted = activitiesCompleted;
      return this;
    }

    public Builder problemsEncountered(String problemsEncountered) {
      this.problemsEncountered = problemsEncountered;
      return this;
    }

    public Builder skillsLearned(String skillsLearned) {
      this.skillsLearned = skillsLearned;
      return this;
    }

    public Builder nextWeekPlan(String nextWeekPlan) {
      this.nextWeekPlan = nextWeekPlan;
      return this;
    }

    public Builder status(WeeklyReportStatus status) {
      this.status = status;
      return this;
    }

    public Builder companyReviewerId(UUID companyReviewerId) {
      this.companyReviewerId = companyReviewerId;
      return this;
    }

    public Builder companyReviewNote(String companyReviewNote) {
      this.companyReviewNote = companyReviewNote;
      return this;
    }

    public Builder academicReviewerId(UUID academicReviewerId) {
      this.academicReviewerId = academicReviewerId;
      return this;
    }

    public Builder academicReviewNote(String academicReviewNote) {
      this.academicReviewNote = academicReviewNote;
      return this;
    }

    public WeeklyReport build() {
      return new WeeklyReport(this);
    }
  }
}