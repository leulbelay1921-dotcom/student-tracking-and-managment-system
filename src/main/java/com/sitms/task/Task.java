package com.sitms.task;

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
 * A task assigned to a student, created by a supervisor, optionally scoped to an internship
 * (SITMS-023).
 *
 * <p>{@code studentId}, {@code internshipId} (nullable) and {@code supervisorId} are plain UUID
 * references to their respective tables. Lifecycle {@code status} is tracked via {@link
 * TaskStatus}. Audit columns and the optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "task")
@EntityListeners(AuditingEntityListener.class)
public class Task {

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

  @Column(name = "title", nullable = false, length = 128)
  private String title;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @Column(name = "category", length = 64)
  private String category;

  @Column(name = "expected_outcome", length = 2000)
  private String expectedOutcome;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private TaskStatus status = TaskStatus.ASSIGNED;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 32)
  private TaskPriority priority = TaskPriority.MEDIUM;

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

  protected Task() {}

  private Task(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.supervisorId = builder.supervisorId;
    this.title = builder.title;
    this.description = builder.description;
    this.startDate = builder.startDate;
    this.dueDate = builder.dueDate;
    this.category = builder.category;
    this.expectedOutcome = builder.expectedOutcome;
    this.status = builder.status;
    this.priority = builder.priority;
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

  public UUID getSupervisorId() {
    return supervisorId;
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

  public LocalDate getDueDate() {
    return dueDate;
  }

  public String getCategory() {
    return category;
  }

  public String getExpectedOutcome() {
    return expectedOutcome;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public TaskPriority getPriority() {
    return priority;
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
      UUID studentId,
      UUID internshipId,
      String title,
      String description,
      LocalDate startDate,
      LocalDate dueDate,
      TaskPriority priority,
      String category,
      String expectedOutcome) {
    this.studentId = studentId;
    this.internshipId = internshipId;
    this.title = title;
    this.description = description;
    this.startDate = startDate;
    this.dueDate = dueDate;
    this.priority = priority;
    this.category = category;
    this.expectedOutcome = expectedOutcome;
  }

  void changeStatus(TaskStatus status) {
    this.status = status;
  }

public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private UUID supervisorId;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate dueDate;
    private String category;
    private String expectedOutcome;
    private TaskStatus status = TaskStatus.ASSIGNED;
    private TaskPriority priority = TaskPriority.MEDIUM;

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

    public Builder dueDate(LocalDate dueDate) {
      this.dueDate = dueDate;
      return this;
    }

    public Builder category(String category) {
      this.category = category;
      return this;
    }

    public Builder expectedOutcome(String expectedOutcome) {
      this.expectedOutcome = expectedOutcome;
      return this;
    }

    public Builder status(TaskStatus status) {
      this.status = status;
      return this;
    }

    public Builder priority(TaskPriority priority) {
      this.priority = priority;
      return this;
    }

    public Task build() {
      return new Task(this);
    }
  }
}
