package com.sitms.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Unit tests for {@link AuditLogService} (SITMS-058): actor attribution from the current JWT
 * (UUID subject → id + name, non-UUID {@code admin} → name only, anonymous → {@code SYSTEM}) and
 * newest-first reads with/without the action filter are pinned against the repository mock.
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

  private static final UUID ACTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID TARGET_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final Pageable PAGEABLE = PageRequest.of(0, 20);

  @Mock private AuditLogRepository auditLogRepository;
  @InjectMocks private AuditLogService auditLogService;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void uuidSubjectIsAttributedAsActorIdAndName() {
    setJwtSubject(ACTOR_ID.toString());

    auditLogService.record(
        AuditAction.ACTIVITY_SUBMITTED, "ACTIVITY", TARGET_ID, "Activity submitted.");

    ArgumentCaptor<AuditLog> captured = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captured.capture());
    AuditLog saved = captured.getValue();
    assertThat(saved.getActorId()).isEqualTo(ACTOR_ID);
    assertThat(saved.getActorName()).isEqualTo(ACTOR_ID.toString());
    assertThat(saved.getAction()).isEqualTo(AuditAction.ACTIVITY_SUBMITTED);
    assertThat(saved.getTargetType()).isEqualTo("ACTIVITY");
    assertThat(saved.getTargetId()).isEqualTo(TARGET_ID);
    assertThat(saved.getDetail()).isEqualTo("Activity submitted.");
  }

  @Test
  void nonUuidAdminSubjectIsAttributedByNameOnly() {
    setJwtSubject("admin");

    auditLogService.record(AuditAction.INTERNSHIP_STATUS_CHANGE, "INTERNSHIP", TARGET_ID, "x");

    ArgumentCaptor<AuditLog> captured = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captured.capture());
    assertThat(captured.getValue().getActorId()).isNull();
    assertThat(captured.getValue().getActorName()).isEqualTo("admin");
  }

  @Test
  void anonymousWriterIsAttributedToSystem() {
    auditLogService.record(AuditAction.DOCUMENT_UPLOADED, "DOCUMENT", TARGET_ID, "x");

    ArgumentCaptor<AuditLog> captured = ArgumentCaptor.forClass(AuditLog.class);
    verify(auditLogRepository).save(captured.capture());
    assertThat(captured.getValue().getActorId()).isNull();
    assertThat(captured.getValue().getActorName()).isEqualTo("SYSTEM");
  }

  @Test
  void listWithoutActionDelegatesToNewestFirstRead() {
    when(auditLogRepository.findAllByOrderByCreatedAtDesc(PAGEABLE)).thenReturn(Page.empty());

    Page<AuditLogResponse> page = auditLogService.list(null, PAGEABLE);

    verify(auditLogRepository).findAllByOrderByCreatedAtDesc(PAGEABLE);
    assertThat(page).isEmpty();
  }

  @Test
  void listWithActionDelegatesToActionFilteredRead() {
    when(auditLogRepository.findByActionOrderByCreatedAtDesc(AuditAction.DOCUMENT_REVIEWED, PAGEABLE))
        .thenReturn(Page.empty());

    auditLogService.list(AuditAction.DOCUMENT_REVIEWED, PAGEABLE);

    verify(auditLogRepository)
        .findByActionOrderByCreatedAtDesc(AuditAction.DOCUMENT_REVIEWED, PAGEABLE);
  }

  @Test
  void listMapsEntitiesToResponseRecords() {
    AuditLog log =
        AuditLog.builder()
            .actorId(ACTOR_ID)
            .actorName(ACTOR_ID.toString())
            .action(AuditAction.EVALUATION_SUBMITTED)
            .targetType("EVALUATION")
            .targetId(TARGET_ID)
            .detail("Submitted.")
            .build();
    when(auditLogRepository.findAllByOrderByCreatedAtDesc(PAGEABLE))
        .thenReturn(new PageImpl<>(List.of(log), PAGEABLE, 1));

    Page<AuditLogResponse> page = auditLogService.list(null, PAGEABLE);

    AuditLogResponse response = page.getContent().get(0);
    assertThat(response.actorId()).isEqualTo(ACTOR_ID);
    assertThat(response.actorName()).isEqualTo(ACTOR_ID.toString());
    assertThat(response.action()).isEqualTo(AuditAction.EVALUATION_SUBMITTED);
    assertThat(response.targetType()).isEqualTo("EVALUATION");
    assertThat(response.targetId()).isEqualTo(TARGET_ID);
    assertThat(response.detail()).isEqualTo("Submitted.");
  }

  private static void setJwtSubject(String sub) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, sub)
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}