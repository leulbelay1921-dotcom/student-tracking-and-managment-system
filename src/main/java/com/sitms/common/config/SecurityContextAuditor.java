package com.sitms.common.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Resolves the current authenticated user's id for auditing.
 *
 * <p>The JWT {@code sub} (a Keycloak user id, which is a UUID) is read from the security context.
 * Once SITMS-006 syncs Keycloak users into {@code app_user}, this id will map 1:1 to
 * {@code app_user.id}; until then the Keycloak {@code sub} is recorded directly so the audit
 * columns are always populated in the security context.
 */
public final class SecurityContextAuditor {

  private SecurityContextAuditor() {}

  /** Returns the current auditor id, or empty when unauthenticated. */
  public static Optional<UUID> currentAuditor() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth instanceof JwtAuthenticationToken token) {
      String sub = token.getToken().getSubject();
      if (sub != null) {
        try {
          return Optional.of(UUID.fromString(sub));
        } catch (IllegalArgumentException ignored) {
          // sub was not a UUID; fall through to the system id below.
        }
      }
    }
    return Optional.empty();
  }
}
