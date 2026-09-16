package com.sitms.common.security;

import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Resolves the {@link PrincipalRole} and subject {@code sub} from a validated {@link
 * JwtAuthenticationToken}. Mirrors the {@code ROLE_*} authorities granted by {@link SecurityConfig}.
 */
public final class PrincipalResolver {

  private static final String ADMIN = "ROLE_ADMIN";
  private static final String SUPERVISOR = "ROLE_SUPERVISOR";
  private static final String FACULTY = "ROLE_FACULTY";

  /** Precedence: ADMIN > SUPERVISOR > FACULTY > STUDENT. */
  public static PrincipalRole roleOf(JwtAuthenticationToken token) {
    boolean admin = has(token, ADMIN);
    if (admin) {
      return PrincipalRole.ADMIN;
    }
    if (has(token, SUPERVISOR)) {
      return PrincipalRole.SUPERVISOR;
    }
    return has(token, FACULTY) ? PrincipalRole.FACULTY : PrincipalRole.STUDENT;
  }

  public static UUID subject(JwtAuthenticationToken token) {
    return UUID.fromString(token.getToken().getSubject());
  }

  private static boolean has(JwtAuthenticationToken token, String authority) {
    return token.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals(authority));
  }

  private PrincipalResolver() {}
}