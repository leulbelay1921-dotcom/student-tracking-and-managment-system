package com.sitms.document;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link Document} metadata (content excluded from the JSON payload). */
public record DocumentResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    String fileName,
    String contentType,
    long size,
    DocumentType type,
    DocumentStatus status,
    UUID reviewerId,
    String reviewNote,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static DocumentResponse from(Document document) {
    return new DocumentResponse(
        document.getId(),
        document.getStudentId(),
        document.getInternshipId(),
        document.getFileName(),
        document.getContentType(),
        document.getSize(),
        document.getType(),
        document.getStatus(),
        document.getReviewerId(),
        document.getReviewNote(),
        document.getCreatedAt(),
        document.getUpdatedAt());
  }
}