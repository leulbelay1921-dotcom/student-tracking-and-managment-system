package com.sitms.ticket;

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
 * A student-issued support/helpdesk ticket linked to their internship (SITMS-073).
 *
 * <p>{@code studentId} and {@code internshipId} are plain UUID references to their tables. A
 * ticket may be assigned to a supervisor or faculty member via {@code assigneeType} +
 * {@code assigneeId}. Audit columns and the optimistic-lock {@code version} follow the V1
 * convention.
 */
@Entity
@Table(name = "ticket")
@EntityListeners(AuditingEntityListener.class)
public class Ticket {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id", nullable = false)
  private UUID internshipId;

  @Column(name = "title", nullable = false, length = 128)
  private String title;

  @Column(name = "description", nullable = false, columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 16)
  private TicketPriority priority = TicketPriority.MEDIUM;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 16)
  private TicketStatus status = TicketStatus.OPEN;

  @Enumerated(EnumType.STRING)
  @Column(name = "assignee_type", length = 16)
  private TicketAssigneeType assigneeType;

  @Column(name = "assignee_id")
  private UUID assigneeId;

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

  protected Ticket() {}

  private Ticket(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.title = builder.title;
    this.description = builder.description;
    this.priority = builder.priority;
    this.status = builder.status;
    this.assigneeType = builder.assigneeType;
    this.assigneeId = builder.assigneeId;
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

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public TicketPriority getPriority() {
    return priority;
  }

  public TicketStatus getStatus() {
    return status;
  }

  public TicketAssigneeType getAssigneeType() {
    return assigneeType;
  }

  public UUID getAssigneeId() {
    return assigneeId;
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
      TicketStatus status, TicketAssigneeType assigneeType, UUID assigneeId) {
    this.status = status;
    this.assigneeType = assigneeType;
    this.assigneeId = assigneeId;
  }

  /** Whether the ticket has reached a terminal state (no further state changes allowed). */
  public boolean isTerminal() {
    return status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED;
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private String title;
    private String description;
    private TicketPriority priority = TicketPriority.MEDIUM;
    private TicketStatus status = TicketStatus.OPEN;
    private TicketAssigneeType assigneeType;
    private UUID assigneeId;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
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

    public Builder priority(TicketPriority priority) {
      this.priority = priority;
      return this;
    }

    public Builder status(TicketStatus status) {
      this.status = status;
      return this;
    }

    public Builder assigneeType(TicketAssigneeType assigneeType) {
      this.assigneeType = assigneeType;
      return this;
    }

    public Builder assigneeId(UUID assigneeId) {
      this.assigneeId = assigneeId;
      return this;
    }

    public Ticket build() {
      return new Ticket(this);
    }
  }
}