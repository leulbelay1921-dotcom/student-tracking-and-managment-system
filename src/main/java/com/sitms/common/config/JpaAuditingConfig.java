package com.sitms.common.config;

import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so entities annotated with {@code @CreatedDate} /
 * {@code @LastModifiedDate} / {@code @CreatedBy} / {@code @LastModifiedBy} are populated
 * automatically on persist/update.
 *
 * <p>The auditor is resolved from the security context. Once Keycloak/Spring Security are wired in
 * (SITMS-005) this will return the authenticated user's id; until then it falls back to a fixed
 * system id so the audit columns are never null.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaAuditingConfig {

  @Bean
  public AuditorAware<UUID> auditorProvider() {
    return () -> Optional.of(SecurityContextAuditor.currentAuditor().orElse(SystemAuditor.SYSTEM_ID));
  }

  /** Placeholder until SITMS-005 wires the real security context auditor. */
  public static final class SystemAuditor {
    public static final UUID SYSTEM_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
  }
}
