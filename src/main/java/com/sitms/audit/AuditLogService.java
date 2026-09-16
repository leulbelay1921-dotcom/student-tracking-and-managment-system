package com.sitms.audit;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and reads the security-relevant audit trail (SITMS-056, EPIC 13).
 *
 * <p>Domain services call {@link #record(AuditAction, String, UUID, String)} at their event
 * boundaries; the caller's transaction is joined so an audit entry is written atomically with the
 * domain change. The actor is resolved from the current JWT: an {@code app_user} UUID subject maps
 * to {@code actorId} + {@code actorName}, the non-UUID {@code admin} system account is recorded by
 * name only, and anonymous/system writes are attributed to {@code SYSTEM}. Reads are admin-only,
 * newest first, optionally filtered by action.
 */
@Service
public class AuditLogService {

  private static final String SYSTEM_ACTOR = "SYSTEM";

  private final AuditLogRepository auditLogRepository;

  public AuditLogService(AuditLogRepository auditLogRepository) {
    this.auditLogRepository = auditLogRepository;
  }

  @Transactional
  public void record(AuditAction action, String targetType, UUID targetId, String detail) {
    String actorName = SYSTEM_ACTOR;
    UUID actorId = null;
    Optional<JwtAuthenticationToken> token = currentToken();
    if (token.isPresent()) {
      String sub = token.get().getToken().getSubject();
      if (sub != null) {
        actorName = sub;
        try {
          actorId = UUID.fromString(sub);
        } catch (IllegalArgumentException ignored) {
          // Non-UUID subject (the 'admin' system account): name only.
        }
      }
    }
    auditLogRepository.save(
        AuditLog.builder()
            .actorId(actorId)
            .actorName(actorName)
            .action(action)
            .targetType(targetType)
            .targetId(targetId)
            .detail(detail)
            .build());
  }

  /** Admin-only read of the audit trail, newest first (admin gate enforced at the controller). */
  @Transactional(readOnly = true)
  public Page<AuditLogResponse> list(AuditAction action, Pageable pageable) {
    Page<AuditLog> page =
        action == null
            ? auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
            : auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    return page.map(AuditLogResponse::from);
  }

  private static Optional<JwtAuthenticationToken> currentToken() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication instanceof JwtAuthenticationToken token) {
      return Optional.of(token);
    }
    return Optional.empty();
  }
}