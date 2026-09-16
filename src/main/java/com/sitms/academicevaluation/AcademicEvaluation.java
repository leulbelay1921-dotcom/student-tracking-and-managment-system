package com.sitms.academicevaluation;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A university academic supervisor's assessment of a student's internship performance (SITMS-078).
 *
 * <p>The form rates six fixed dimensions from 1 (poor) to 5 (excellent) — Academic Learning,
 * Problem Solving, Documentation, Technical Development, Professionalism and Final Presentation —
 * and one record exists per internship ({@code internship_id} is unique). {@code totalScore}
 * (0-100) is computed from the six dimension scores (mean scaled to a percentage, rounded) and
 * stored so reports and analytics need no derivation at read time. {@code evaluatorId} references
 * the reviewing academic staff member; {@code internshipId} and {@code studentId} are required
 * plain UUID references. Audit columns and the optimistic-lock {@code version} follow the V1
 * convention.
 */
@Entity
@Table(name = "academic_evaluation")
@EntityListeners(AuditingEntityListener.class)
public class AcademicEvaluation {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id", nullable = false, unique = true)
  private UUID internshipId;

  @Column(name = "evaluator_id", nullable = false)
  private UUID evaluatorId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "academic_learning", nullable = false)
  private int academicLearning;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "problem_solving", nullable = false)
  private int problemSolving;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "documentation", nullable = false)
  private int documentation;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "technical_development", nullable = false)
  private int technicalDevelopment;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "professionalism", nullable = false)
  private int professionalism;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "final_presentation", nullable = false)
  private int finalPresentation;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "total_score", nullable = false)
  private int totalScore;

  @Column(name = "remarks")
  private String remarks;

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

  protected AcademicEvaluation() {}

  private AcademicEvaluation(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.evaluatorId = builder.evaluatorId;
    this.academicLearning = builder.academicLearning;
    this.problemSolving = builder.problemSolving;
    this.documentation = builder.documentation;
    this.technicalDevelopment = builder.technicalDevelopment;
    this.professionalism = builder.professionalism;
    this.finalPresentation = builder.finalPresentation;
    this.remarks = builder.remarks;
    this.totalScore =
        computeTotalScore(
            academicLearning, problemSolving, documentation, technicalDevelopment,
            professionalism, finalPresentation);
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Computes the overall percentage (0-100) from the six 1-5 dimension scores: mean scaled to a
   * percentage, rounded to the nearest integer.
   */
  static int computeTotalScore(
      int academicLearning, int problemSolving, int documentation, int technicalDevelopment,
      int professionalism, int finalPresentation) {
    double mean =
        (academicLearning + problemSolving + documentation + technicalDevelopment
                + professionalism + finalPresentation)
            / 6.0;
    return (int) Math.round(mean * 20.0);
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

  public UUID getEvaluatorId() {
    return evaluatorId;
  }

  public int getAcademicLearning() {
    return academicLearning;
  }

  public int getProblemSolving() {
    return problemSolving;
  }

  public int getDocumentation() {
    return documentation;
  }

  public int getTechnicalDevelopment() {
    return technicalDevelopment;
  }

  public int getProfessionalism() {
    return professionalism;
  }

  public int getFinalPresentation() {
    return finalPresentation;
  }

  public int getTotalScore() {
    return totalScore;
  }

  public String getRemarks() {
    return remarks;
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
      int academicLearning,
      int problemSolving,
      int documentation,
      int technicalDevelopment,
      int professionalism,
      int finalPresentation,
      String remarks) {
    this.academicLearning = academicLearning;
    this.problemSolving = problemSolving;
    this.documentation = documentation;
    this.technicalDevelopment = technicalDevelopment;
    this.professionalism = professionalism;
    this.finalPresentation = finalPresentation;
    this.remarks = remarks;
    this.totalScore =
        computeTotalScore(
            academicLearning, problemSolving, documentation, technicalDevelopment,
            professionalism, finalPresentation);
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private UUID evaluatorId;
    private int academicLearning;
    private int problemSolving;
    private int documentation;
    private int technicalDevelopment;
    private int professionalism;
    private int finalPresentation;
    private String remarks;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
      return this;
    }

    public Builder evaluatorId(UUID evaluatorId) {
      this.evaluatorId = evaluatorId;
      return this;
    }

    public Builder academicLearning(int academicLearning) {
      this.academicLearning = academicLearning;
      return this;
    }

    public Builder problemSolving(int problemSolving) {
      this.problemSolving = problemSolving;
      return this;
    }

    public Builder documentation(int documentation) {
      this.documentation = documentation;
      return this;
    }

    public Builder technicalDevelopment(int technicalDevelopment) {
      this.technicalDevelopment = technicalDevelopment;
      return this;
    }

    public Builder professionalism(int professionalism) {
      this.professionalism = professionalism;
      return this;
    }

    public Builder finalPresentation(int finalPresentation) {
      this.finalPresentation = finalPresentation;
      return this;
    }

    public Builder remarks(String remarks) {
      this.remarks = remarks;
      return this;
    }

    public AcademicEvaluation build() {
      return new AcademicEvaluation(this);
    }
  }
}