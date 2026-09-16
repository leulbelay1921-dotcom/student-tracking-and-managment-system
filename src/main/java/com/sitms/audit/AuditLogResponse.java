package com.sitms.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link AuditLog}. */
public record AuditLogResponse(
    UUID id,
    UUID actorId,
    String actorName,
    AuditAction action,
    String targetType,
    UUID targetId,
    String detail,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static AuditLogResponse from(AuditLog auditLog) {
    return new AuditLogResponse(
        auditLog.getId(),
        auditLog.getActorId(),
        auditLog.getActorName(),
        auditLog.getAction(),
        auditLog.getTargetType(),
        auditLog.getTargetId(),
        auditLog.getDetail(),
        auditLog.getCreatedAt(),
        auditLog.getUpdatedAt());
  }
}