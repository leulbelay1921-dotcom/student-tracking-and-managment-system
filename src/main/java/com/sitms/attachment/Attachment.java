package com.sitms.attachment;

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
 * Links a stored {@link com.sitms.document.Document} to an owning record (an activity or a weekly
 * report). File bytes live in {@code document.content}; this table only records the association so
 * that attachments are reused rather than duplicated.
 *
 * <p>{@code ownerId} is a plain UUID reference to the owning entity (not a JPA relationship, so the
 * owning modules stay decoupled). Audit columns and the optimistic-lock {@code version} follow the
 * V1 convention.
 */
@Entity
@Table(name = "attachment")
@EntityListeners(AuditingEntityListener.class)
public class Attachment {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", nullable = false, length = 32)
  private AttachmentOwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "document_id", nullable = false)
  private UUID documentId;

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

  protected Attachment() {}

  private Attachment(Builder builder) {
    this.ownerType = builder.ownerType;
    this.ownerId = builder.ownerId;
    this.documentId = builder.documentId;
  }

  public static Builder builder() {
    return new Builder();
  }

  public UUID getId() {
    return id;
  }

  public AttachmentOwnerType getOwnerType() {
    return ownerType;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public UUID getDocumentId() {
    return documentId;
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
    private AttachmentOwnerType ownerType;
    private UUID ownerId;
    private UUID documentId;

    private Builder() {}

    public Builder ownerType(AttachmentOwnerType ownerType) {
      this.ownerType = ownerType;
      return this;
    }

    public Builder ownerId(UUID ownerId) {
      this.ownerId = ownerId;
      return this;
    }

    public Builder documentId(UUID documentId) {
      this.documentId = documentId;
      return this;
    }

    public Attachment build() {
      return new Attachment(this);
    }
  }
}
