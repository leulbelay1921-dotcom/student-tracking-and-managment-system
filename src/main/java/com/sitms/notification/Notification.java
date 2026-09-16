package com.sitms.notification;

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
 * An in-app notification addressed to a single user (SITMS-050, EPIC 12).
 *
 * <p>Broad-cast to the {@code app_user} row owned by the recipient. {@code type} records the
 * domain event that produced it (reused by the SITMS-053 email channel), {@code link} is the
 * frontend route the shell should navigate to when the notification is opened, and {@code read}
 * tracks read-state. Audit columns and the optimistic-lock {@code version} follow the V1
 * convention.
 */
@Entity
@Table(name = "notification")
@EntityListeners(AuditingEntityListener.class)
public class Notification {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 64)
  private NotificationType type;

  @Column(name = "title", nullable = false)
  private String title;

  @Column(name = "body")
  private String body;

  @Column(name = "link")
  private String link;

  @Column(name = "read", nullable = false)
  private boolean read;

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

  protected Notification() {}

  private Notification(Builder builder) {
    this.recipientId = builder.recipientId;
    this.type = builder.type;
    this.title = builder.title;
    this.body = builder.body;
    this.link = builder.link;
  }

  public static Builder builder() {
    return new Builder();
  }

  void markRead() {
    this.read = true;
  }

  public UUID getId() {
    return id;
  }

  public UUID getRecipientId() {
    return recipientId;
  }

  public NotificationType getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getBody() {
    return body;
  }

  public String getLink() {
    return link;
  }

  public boolean isRead() {
    return read;
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
    private UUID recipientId;
    private NotificationType type;
    private String title;
    private String body;
    private String link;

    private Builder() {}

    public Builder recipientId(UUID recipientId) {
      this.recipientId = recipientId;
      return this;
    }

    public Builder type(NotificationType type) {
      this.type = type;
      return this;
    }

    public Builder title(String title) {
      this.title = title;
      return this;
    }

    public Builder body(String body) {
      this.body = body;
      return this;
    }

    public Builder link(String link) {
      this.link = link;
      return this;
    }

    public Notification build() {
      return new Notification(this);
    }
  }
}