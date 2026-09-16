package com.sitms.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository integration test for {@link AuditLog} against a real Postgres (SITMS-056): persist/
 * find round-trip, audit columns, newest-first ordering, and action filtering.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class AuditLogRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID ACTOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID TARGET_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private AuditLogRepository auditLogRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedActor() {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, 'SUPERVISOR', TRUE, ?)",
        ACTOR_ID,
        "actor1",
        "actor1@example.com",
        "Actor One",
        AUDITOR_ID);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persistsAndRetrievesAuditLog() {
    setAuditor(AUDITOR_ID);
    AuditLog saved =
        auditLogRepository.save(
            AuditLog.builder()
                .actorId(ACTOR_ID)
                .actorName(ACTOR_ID.toString())
                .action(AuditAction.INTERNSHIP_STATUS_CHANGE)
                .targetType("INTERNSHIP")
                .targetId(TARGET_ID)
                .detail("Internship status changed from PENDING to APPROVED.")
                .build());

    AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getActorId()).isEqualTo(ACTOR_ID);
    assertThat(found.getActorName()).isEqualTo(ACTOR_ID.toString());
    assertThat(found.getAction()).isEqualTo(AuditAction.INTERNSHIP_STATUS_CHANGE);
    assertThat(found.getTargetType()).isEqualTo("INTERNSHIP");
    assertThat(found.getTargetId()).isEqualTo(TARGET_ID);
    assertThat(found.getDetail()).isEqualTo("Internship status changed from PENDING to APPROVED.");
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    AuditLog saved = auditLogRepository.save(log(ACTOR_ID, AuditAction.ACTIVITY_SUBMITTED).build());
    auditLogRepository.flush();

    AuditLog found = auditLogRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void listsNewestFirst() {
    setAuditor(AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO audit_log (id, actor_id, actor_name, action, target_type, target_id, detail,"
            + " created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, DATEADD('HOUR', -2, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP)",
        UUID.randomUUID(),
        ACTOR_ID,
        "actor",
        "INTERNSHIP_STATUS_CHANGE",
        "INTERNSHIP",
        TARGET_ID,
        "older",
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO audit_log (id, actor_id, actor_name, action, target_type, target_id, detail,"
            + " created_by, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, DATEADD('HOUR', -1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP)",
        UUID.randomUUID(),
        ACTOR_ID,
        "actor",
        "ACTIVITY_REVIEWED",
        "ACTIVITY",
        TARGET_ID,
        "newer",
        AUDITOR_ID);

    Page<AuditLog> page =
        auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent().get(0).getDetail()).isEqualTo("newer");
    assertThat(page.getContent().get(1).getDetail()).isEqualTo("older");
  }

  @Test
  void filtersByAction() {
    setAuditor(AUDITOR_ID);
    auditLogRepository.save(log(ACTOR_ID, AuditAction.DOCUMENT_UPLOADED).build());
    auditLogRepository.save(log(ACTOR_ID, AuditAction.EVALUATION_SUBMITTED).build());
    auditLogRepository.save(log(ACTOR_ID, AuditAction.DOCUMENT_UPLOADED).build());

    Page<AuditLog> page =
        auditLogRepository.findByActionOrderByCreatedAtDesc(
            AuditAction.DOCUMENT_UPLOADED, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).allMatch(l -> l.getAction() == AuditAction.DOCUMENT_UPLOADED);
  }

  private static AuditLog.Builder log(UUID actorId, AuditAction action) {
    return AuditLog.builder()
        .actorId(actorId)
        .actorName(actorId.toString())
        .action(action)
        .targetType("ACTIVITY")
        .targetId(UUID.randomUUID())
        .detail("Audit record.");
  }

  private void setAuditor(UUID id) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, id.toString())
            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}