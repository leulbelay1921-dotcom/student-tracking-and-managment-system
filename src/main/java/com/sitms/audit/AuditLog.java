package com.sitms.audit;

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
 * An append-only record of a security-relevant action (SITMS-056, EPIC 13).
 *
 * <p>{@code actorName} is the human-readable principal — the JWT subject, which is an
 * {@code app_user} UUID for students/supervisors or the non-UUID {@code admin} system account
 * string. {@code action} records the domain event, {@code targetType}/{@code targetId} identify the
 * affected row, and {@code detail} is a human-readable description. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "actor_id")
  private UUID actorId;

  @Column(name = "actor_name", nullable = false, length = 128)
  private String actorName;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 64)
  private AuditAction action;

  @Column(name = "target_type", nullable = false, length = 64)
  private String targetType;

  @Column(name = "target_id")
  private UUID targetId;

  @Column(name = "detail", nullable = false, length = 2000)
  private String detail;

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

  protected AuditLog() {}

  private AuditLog(Builder builder) {
    this.actorId = builder.actorId;
    this.actorName = builder.actorName;
    this.action = builder.action;
    this.targetType = builder.targetType;
    this.targetId = builder.targetId;
    this.detail = builder.detail;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public UUID getActorId() {
    return actorId;
  }

  public String getActorName() {
    return actorName;
  }

  public AuditAction getAction() {
    return action;
  }

  public String getTargetType() {
    return targetType;
  }

  public UUID getTargetId() {
    return targetId;
  }

  public String getDetail() {
    return detail;
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

  public static final class Builder {
    private UUID actorId;
    private String actorName;
    private AuditAction action;
    private String targetType;
    private UUID targetId;
    private String detail;

    private Builder() {}

    public Builder actorId(UUID actorId) {
      this.actorId = actorId;
      return this;
    }

    public Builder actorName(String actorName) {
      this.actorName = actorName;
      return this;
    }

    public Builder action(AuditAction action) {
      this.action = action;
      return this;
    }

    public Builder targetType(String targetType) {
      this.targetType = targetType;
      return this;
    }

    public Builder targetId(UUID targetId) {
      this.targetId = targetId;
      return this;
    }

    public Builder detail(String detail) {
      this.detail = detail;
      return this;
    }

    public AuditLog build() {
      return new AuditLog(this);
    }
  }
}