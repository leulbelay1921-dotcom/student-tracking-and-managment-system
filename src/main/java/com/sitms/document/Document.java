package com.sitms.document;

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
 * A file a student submits during their internship (offer letter, report, evidence), subject to
 * type/size validation (SITMS-037) and a review workflow (SITMS-039).
 *
 * <p>{@code studentId} is required; {@code internshipId} is an optional plain UUID reference.
 * Content is stored inline as raw bytes so the app needs no external object storage. During review
 * {@code reviewerId}/{@code reviewNote} capture the supervisor's decision. Audit columns and the
 * optimistic-lock {@code version} follow the V1 convention.
 */
@Entity
@Table(name = "document")
@EntityListeners(AuditingEntityListener.class)
public class Document {

  @Id
  @GeneratedValue
  @UuidGenerator
  private UUID id;

  @Column(name = "student_id", nullable = false)
  private UUID studentId;

  @Column(name = "internship_id")
  private UUID internshipId;

  @Column(name = "file_name", nullable = false)
  private String fileName;

  @Column(name = "content_type", nullable = false)
  private String contentType;

  @Column(name = "size", nullable = false)
  private long size;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 32)
  private DocumentType type = DocumentType.OTHER;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private DocumentStatus status = DocumentStatus.PENDING;

  @Column(name = "reviewer_id")
  private UUID reviewerId;

  @Column(name = "review_note")
  private String reviewNote;

  @Column(name = "content", nullable = false, columnDefinition = "BYTEA")
  private byte[] content;

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

  protected Document() {}

  private Document(Builder builder) {
    this.studentId = builder.studentId;
    this.internshipId = builder.internshipId;
    this.fileName = builder.fileName;
    this.contentType = builder.contentType;
    this.size = builder.size;
    this.type = builder.type;
    this.status = builder.status;
    this.reviewerId = builder.reviewerId;
    this.reviewNote = builder.reviewNote;
    this.content = builder.content;
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

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSize() {
    return size;
  }

  public DocumentType getType() {
    return type;
  }

  public DocumentStatus getStatus() {
    return status;
  }

  public UUID getReviewerId() {
    return reviewerId;
  }

  public String getReviewNote() {
    return reviewNote;
  }

  public byte[] getContent() {
    return content;
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

  void updateReview(UUID reviewerId, DocumentStatus status, String reviewNote) {
    this.reviewerId = reviewerId;
    this.status = status;
    this.reviewNote = reviewNote;
  }

  public static final class Builder {
    private UUID studentId;
    private UUID internshipId;
    private String fileName;
    private String contentType;
    private long size;
    private DocumentType type = DocumentType.OTHER;
    private DocumentStatus status = DocumentStatus.PENDING;
    private UUID reviewerId;
    private String reviewNote;
    private byte[] content;

    private Builder() {}

    public Builder studentId(UUID studentId) {
      this.studentId = studentId;
      return this;
    }

    public Builder internshipId(UUID internshipId) {
      this.internshipId = internshipId;
      return this;
    }

    public Builder fileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder contentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder size(long size) {
      this.size = size;
      return this;
    }

    public Builder type(DocumentType type) {
      this.type = type;
      return this;
    }

    public Builder status(DocumentStatus status) {
      this.status = status;
      return this;
    }

    public Builder reviewerId(UUID reviewerId) {
      this.reviewerId = reviewerId;
      return this;
    }

    public Builder reviewNote(String reviewNote) {
      this.reviewNote = reviewNote;
      return this;
    }

    public Builder content(byte[] content) {
      this.content = content;
      return this;
    }

    public Document build() {
      return new Document(this);
    }
  }
}