package com.sitms.evaluation;

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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A supervisor's assessment of a student's performance during an internship (SITMS-040, EPIC 10).
 *
 * <p>The review form rates five fixed dimensions from 1 (poor) to 5 (excellent) and captures up to
 * three free-text fields (strengths, areas for improvement, general comments). {@code overallScore}
 * (0-100) is computed from the dimension scores (mean scaled to a percentage, rounded) and stored
 * so reports and analytics need no derivation at read time. {@code studentId}/{@code supervisorId}
 * are required; {@code internshipId} is an optional plain UUID reference. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "evaluation")
@EntityListeners(AuditingEntityListener.class)
public class Evaluation {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id")
  private UUID internshipId;

  @Column(name = "supervisor_id", nullable = false)
  private UUID supervisorId;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "technical_skills", nullable = false)
  private int technicalSkills;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "communication", nullable = false)
  private int communication;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "teamwork", nullable = false)
  private int teamwork;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "problem_solving", nullable = false)
  private int problemSolving;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "professionalism", nullable = false)
  private int professionalism;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "punctuality")
  private Integer punctuality;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "reliability")
  private Integer reliability;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "independence_work")
  private Integer independenceWork;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "communication_skills")
  private Integer communicationSkills;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "professional_conduct")
  private Integer professionalConduct;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "learning_speed")
  private Integer learningSpeed;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "commitment")
  private Integer commitment;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "accuracy")
  private Integer accuracy;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "team_contribution")
  private Integer teamContribution;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "cooperation")
  private Integer cooperation;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "technical_proficiency")
  private Integer technicalProficiency;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "organizational_skills")
  private Integer organizationalSkills;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "project_support")
  private Integer projectSupport;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "task_responsibility")
  private Integer taskResponsibility;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "quality_team_member")
  private Integer qualityTeamMember;

  @Column(name = "remarks")
  private String remarks;

  @Column(name = "department")
  private String department;

  @Column(name = "company_name")
  private String companyName;

  @Column(name = "strengths")
  private String strengths;

  @Column(name = "improvement")
  private String improvement;

  @Column(name = "comments")
  private String comments;

  @JdbcTypeCode(SqlTypes.SMALLINT)
  @Column(name = "overall_score", nullable = false)
  private int overallScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private EvaluationStatus status = EvaluationStatus.DRAFT;

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

  protected Evaluation() {}

  private Evaluation(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.supervisorId = builder.supervisorId;
    this.technicalSkills = builder.technicalSkills;
    this.communication = builder.communication;
    this.teamwork = builder.teamwork;
    this.problemSolving = builder.problemSolving;
    this.professionalism = builder.professionalism;
    this.punctuality = builder.punctuality;
    this.reliability = builder.reliability;
    this.independenceWork = builder.independenceWork;
    this.communicationSkills = builder.communicationSkills;
    this.professionalConduct = builder.professionalConduct;
    this.learningSpeed = builder.learningSpeed;
    this.commitment = builder.commitment;
    this.accuracy = builder.accuracy;
    this.teamContribution = builder.teamContribution;
    this.cooperation = builder.cooperation;
    this.technicalProficiency = builder.technicalProficiency;
    this.organizationalSkills = builder.organizationalSkills;
    this.projectSupport = builder.projectSupport;
    this.taskResponsibility = builder.taskResponsibility;
    this.qualityTeamMember = builder.qualityTeamMember;
    this.remarks = builder.remarks;
    this.department = builder.department;
    this.companyName = builder.companyName;
    this.strengths = builder.strengths;
    this.improvement = builder.improvement;
    this.comments = builder.comments;
    this.overallScore = builder.overallScore;
    this.status = builder.status;
  }

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Computes the overall percentage (0-100) from the five 1-5 dimension scores: mean scaled to a
   * percentage, rounded to the nearest integer.
   */
  static int computeOverallScore(
      int technicalSkills, int communication, int teamwork, int problemSolving, int professionalism) {
    double mean =
        (technicalSkills + communication + teamwork + problemSolving + professionalism) / 5.0;
    return (int) Math.round(mean * 20.0);
  }

  static boolean allPaperPresent(
      Integer punctuality, Integer reliability, Integer independenceWork,
      Integer communicationSkills, Integer professionalConduct,
      Integer learningSpeed, Integer commitment, Integer accuracy,
      Integer teamContribution, Integer cooperation,
      Integer technicalProficiency, Integer organizationalSkills,
      Integer projectSupport, Integer taskResponsibility, Integer qualityTeamMember) {
    return punctuality != null && reliability != null && independenceWork != null
        && communicationSkills != null && professionalConduct != null
        && learningSpeed != null && commitment != null && accuracy != null
        && teamContribution != null && cooperation != null
        && technicalProficiency != null && organizationalSkills != null
        && projectSupport != null && taskResponsibility != null && qualityTeamMember != null;
  }

  static int computeOverallScore(
      int technicalSkills, int communication, int teamwork, int problemSolving, int professionalism,
      Integer punctuality, Integer reliability, Integer independenceWork,
      Integer communicationSkills, Integer professionalConduct,
      Integer learningSpeed, Integer commitment, Integer accuracy,
      Integer teamContribution, Integer cooperation,
      Integer technicalProficiency, Integer organizationalSkills,
      Integer projectSupport, Integer taskResponsibility, Integer qualityTeamMember) {
    if (allPaperPresent(punctuality, reliability, independenceWork, communicationSkills,
        professionalConduct, learningSpeed, commitment, accuracy, teamContribution, cooperation,
        technicalProficiency, organizationalSkills, projectSupport, taskResponsibility, qualityTeamMember)) {
      return punctuality + reliability + independenceWork + communicationSkills
          + professionalConduct + learningSpeed + commitment + accuracy
          + teamContribution + cooperation + technicalProficiency + organizationalSkills
          + projectSupport + taskResponsibility + qualityTeamMember;
    }
    return computeOverallScore(technicalSkills, communication, teamwork, problemSolving, professionalism);
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

  public UUID getSupervisorId() {
    return supervisorId;
  }

  public int getTechnicalSkills() {
    return technicalSkills;
  }

  public int getCommunication() {
    return communication;
  }

  public int getTeamwork() {
    return teamwork;
  }

  public int getProblemSolving() {
    return problemSolving;
  }

  public int getProfessionalism() {
    return professionalism;
  }

  public Integer getPunctuality() {
    return punctuality;
  }

  public Integer getReliability() {
    return reliability;
  }

  public Integer getIndependenceWork() {
    return independenceWork;
  }

  public Integer getCommunicationSkills() {
    return communicationSkills;
  }

  public Integer getProfessionalConduct() {
    return professionalConduct;
  }

  public Integer getLearningSpeed() {
    return learningSpeed;
  }

  public Integer getCommitment() {
    return commitment;
  }

  public Integer getAccuracy() {
    return accuracy;
  }

  public Integer getTeamContribution() {
    return teamContribution;
  }

  public Integer getCooperation() {
    return cooperation;
  }

  public Integer getTechnicalProficiency() {
    return technicalProficiency;
  }

  public Integer getOrganizationalSkills() {
    return organizationalSkills;
  }

  public Integer getProjectSupport() {
    return projectSupport;
  }

  public Integer getTaskResponsibility() {
    return taskResponsibility;
  }

  public Integer getQualityTeamMember() {
    return qualityTeamMember;
  }

  public String getRemarks() {
    return remarks;
  }

  public String getDepartment() {
    return department;
  }

  public String getCompanyName() {
    return companyName;
  }

  public String getStrengths() {
    return strengths;
  }

  public String getImprovement() {
    return improvement;
  }

  public String getComments() {
    return comments;
  }

  public int getOverallScore() {
    return overallScore;
  }

  public EvaluationStatus getStatus() {
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
      EvaluationStatus status,
      int technicalSkills,
      int communication,
      int teamwork,
      int problemSolving,
      int professionalism,
      String strengths,
      String improvement,
      String comments,
      Integer punctuality, Integer reliability, Integer independenceWork, Integer communicationSkills,
      Integer professionalConduct, Integer learningSpeed, Integer commitment, Integer accuracy,
      Integer teamContribution, Integer cooperation,
      Integer technicalProficiency, Integer organizationalSkills,
      Integer projectSupport, Integer taskResponsibility, Integer qualityTeamMember,
      String remarks, String department, String companyName) {
    this.status = status;
    this.technicalSkills = technicalSkills;
    this.communication = communication;
    this.teamwork = teamwork;
    this.problemSolving = problemSolving;
    this.professionalism = professionalism;
    this.strengths = strengths;
    this.improvement = improvement;
    this.comments = comments;
    this.punctuality = punctuality;
    this.reliability = reliability;
    this.independenceWork = independenceWork;
    this.communicationSkills = communicationSkills;
    this.professionalConduct = professionalConduct;
    this.learningSpeed = learningSpeed;
    this.commitment = commitment;
    this.accuracy = accuracy;
    this.teamContribution = teamContribution;
    this.cooperation = cooperation;
    this.technicalProficiency = technicalProficiency;
    this.organizationalSkills = organizationalSkills;
    this.projectSupport = projectSupport;
    this.taskResponsibility = taskResponsibility;
    this.qualityTeamMember = qualityTeamMember;
    this.remarks = remarks;
    this.department = department;
    this.companyName = companyName;
    this.overallScore =
        computeOverallScore(
            technicalSkills, communication, teamwork, problemSolving, professionalism,
            punctuality, reliability, independenceWork, communicationSkills, professionalConduct,
            learningSpeed, commitment, accuracy, teamContribution, cooperation,
            technicalProficiency, organizationalSkills, projectSupport, taskResponsibility,
            qualityTeamMember);
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private UUID supervisorId;
    private int technicalSkills;
    private int communication;
    private int teamwork;
    private int problemSolving;
    private int professionalism;
    private Integer punctuality;
    private Integer reliability;
    private Integer independenceWork;
    private Integer communicationSkills;
    private Integer professionalConduct;
    private Integer learningSpeed;
    private Integer commitment;
    private Integer accuracy;
    private Integer teamContribution;
    private Integer cooperation;
    private Integer technicalProficiency;
    private Integer organizationalSkills;
    private Integer projectSupport;
    private Integer taskResponsibility;
    private Integer qualityTeamMember;
    private String remarks;
    private String department;
    private String companyName;
    private String strengths;
    private String improvement;
    private String comments;
    private int overallScore;
    private EvaluationStatus status = EvaluationStatus.DRAFT;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
      return this;
    }

    public Builder supervisorId(UUID supervisorId) {
      this.supervisorId = supervisorId;
      return this;
    }

    public Builder technicalSkills(int technicalSkills) {
      this.technicalSkills = technicalSkills;
      return this;
    }

    public Builder communication(int communication) {
      this.communication = communication;
      return this;
    }

    public Builder teamwork(int teamwork) {
      this.teamwork = teamwork;
      return this;
    }

    public Builder problemSolving(int problemSolving) {
      this.problemSolving = problemSolving;
      return this;
    }

    public Builder professionalism(int professionalism) {
      this.professionalism = professionalism;
      return this;
    }

    public Builder punctuality(Integer punctuality) {
      this.punctuality = punctuality;
      return this;
    }

    public Builder reliability(Integer reliability) {
      this.reliability = reliability;
      return this;
    }

    public Builder independenceWork(Integer independenceWork) {
      this.independenceWork = independenceWork;
      return this;
    }

    public Builder communicationSkills(Integer communicationSkills) {
      this.communicationSkills = communicationSkills;
      return this;
    }

    public Builder professionalConduct(Integer professionalConduct) {
      this.professionalConduct = professionalConduct;
      return this;
    }

    public Builder learningSpeed(Integer learningSpeed) {
      this.learningSpeed = learningSpeed;
      return this;
    }

    public Builder commitment(Integer commitment) {
      this.commitment = commitment;
      return this;
    }

    public Builder accuracy(Integer accuracy) {
      this.accuracy = accuracy;
      return this;
    }

    public Builder teamContribution(Integer teamContribution) {
      this.teamContribution = teamContribution;
      return this;
    }

    public Builder cooperation(Integer cooperation) {
      this.cooperation = cooperation;
      return this;
    }

    public Builder technicalProficiency(Integer technicalProficiency) {
      this.technicalProficiency = technicalProficiency;
      return this;
    }

    public Builder organizationalSkills(Integer organizationalSkills) {
      this.organizationalSkills = organizationalSkills;
      return this;
    }

    public Builder projectSupport(Integer projectSupport) {
      this.projectSupport = projectSupport;
      return this;
    }

    public Builder taskResponsibility(Integer taskResponsibility) {
      this.taskResponsibility = taskResponsibility;
      return this;
    }

    public Builder qualityTeamMember(Integer qualityTeamMember) {
      this.qualityTeamMember = qualityTeamMember;
      return this;
    }

    public Builder remarks(String remarks) {
      this.remarks = remarks;
      return this;
    }

    public Builder department(String department) {
      this.department = department;
      return this;
    }

    public Builder companyName(String companyName) {
      this.companyName = companyName;
      return this;
    }

    public Builder strengths(String strengths) {
      this.strengths = strengths;
      return this;
    }

    public Builder improvement(String improvement) {
      this.improvement = improvement;
      return this;
    }

    public Builder comments(String comments) {
      this.comments = comments;
      return this;
    }

    public Builder status(EvaluationStatus status) {
      this.status = status;
      return this;
    }

    public Evaluation build() {
      this.overallScore =
          computeOverallScore(
              technicalSkills, communication, teamwork, problemSolving, professionalism,
              punctuality, reliability, independenceWork, communicationSkills, professionalConduct,
              learningSpeed, commitment, accuracy, teamContribution, cooperation,
              technicalProficiency, organizationalSkills, projectSupport, taskResponsibility,
              qualityTeamMember);
      return new Evaluation(this);
    }
  }
}