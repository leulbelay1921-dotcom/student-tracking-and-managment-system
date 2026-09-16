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
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * A supervisor's review of an {@link Activity}, 1:1 with the activity (SITMS-027).
 *
 * <p>{@code activityId} and {@code reviewerId} (supervisor) are plain UUID references. The {@code
 * decision} ({@link ReviewDecision}) and optional {@code feedback} record the outcome. Audit
 * columns and the optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "activity_review")
@EntityListeners(AuditingEntityListener.class)
public class ActivityReview {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "activity_id", nullable = false, unique = true)
  private UUID activityId;

  @Column(name = "reviewer_id", nullable = false)
  private UUID reviewerId;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private ReviewDecision decision;

  @Column(name = "feedback", columnDefinition = "TEXT")
  private String feedback;

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

  protected ActivityReview() {}

  private ActivityReview(Builder builder) {
    this.activityId = builder.activityId;
    this.reviewerId = builder.reviewerId;
    this.decision = builder.decision;
    this.feedback = builder.feedback;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public UUID getActivityId() {
    return activityId;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public ReviewDecision getDecision() {
    return decision;
  }

  public String getFeedback() {
    return feedback;
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

  void update(ReviewDecision decision, String feedback) {
    this.decision = decision;
    this.feedback = feedback;
  }

  public static final class Builder {
    private UUID activityId;
    private UUID reviewerId;
    private ReviewDecision decision;
    private String feedback;

    private Builder() {}

    public Builder activityId(UUID activityId) {
      this.activityId = activityId;
      return this;
    }

    public Builder reviewerId(UUID reviewerId) {
      this.reviewerId = reviewerId;
      return this;
    }

    public Builder decision(ReviewDecision decision) {
      this.decision = decision;
      return this;
    }

    public Builder feedback(String feedback) {
      this.feedback = feedback;
      return this;
    }

    public ActivityReview build() {
      return new ActivityReview(this);
    }
  }
}
