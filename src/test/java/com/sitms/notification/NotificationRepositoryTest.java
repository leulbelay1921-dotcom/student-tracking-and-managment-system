package com.sitms.notification;

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
 * Repository integration test for {@link Notification} against a real Postgres (SITMS-050):
 * persist/find round-trip, audit columns, read-state transition, and recipient-scoped queries.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class NotificationRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_RECIPIENT_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private NotificationRepository notificationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedRecipients() {
    seedAppUser(RECIPIENT_ID, "recipient1");
    seedAppUser(OTHER_RECIPIENT_ID, "recipient2");
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void seedAppUser(UUID id, String username) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, 'STUDENT', TRUE, ?)",
        id,
        username,
        username + "@example.com",
        username,
        AUDITOR_ID);
  }

  @Test
  void persistsAndRetrievesNotification() {
    setAuditor(AUDITOR_ID);
    Notification saved =
        notificationRepository.save(
            notification(RECIPIENT_ID, NotificationType.ACTIVITY_REVIEWED)
                .title("Activity approved")
                .body("Your daily log was approved by the supervisor.")
                .link("/activities/123")
                .build());

    Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getRecipientId()).isEqualTo(RECIPIENT_ID);
    assertThat(found.getType()).isEqualTo(NotificationType.ACTIVITY_REVIEWED);
    assertThat(found.getTitle()).isEqualTo("Activity approved");
    assertThat(found.getBody()).isEqualTo("Your daily log was approved by the supervisor.");
    assertThat(found.getLink()).isEqualTo("/activities/123");
    assertThat(found.isRead()).isFalse();
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Notification saved = notificationRepository.save(notification(RECIPIENT_ID, NotificationType.ACTIVITY_SUBMITTED).build());
    notificationRepository.flush();

    Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void marksNotificationAsRead() {
    setAuditor(AUDITOR_ID);
    Notification saved =
        notificationRepository.saveAndFlush(notification(RECIPIENT_ID, NotificationType.EVALUATION_SUBMITTED).build());
    assertThat(saved.isRead()).isFalse();

    saved.markRead();
    notificationRepository.saveAndFlush(saved);

    Notification found = notificationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.isRead()).isTrue();
  }

  @Test
  void findsByRecipientPaged() {
    setAuditor(AUDITOR_ID);
    notificationRepository.save(notification(RECIPIENT_ID, NotificationType.ACTIVITY_REVIEWED).build());
    notificationRepository.save(notification(RECIPIENT_ID, NotificationType.EVALUATION_SUBMITTED).build());
    notificationRepository.save(notification(OTHER_RECIPIENT_ID, NotificationType.ACTIVITY_REVIEWED).build());

    Page<Notification> page =
        notificationRepository.findByRecipientId(RECIPIENT_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).allMatch(n -> n.getRecipientId().equals(RECIPIENT_ID));
  }

  @Test
  void filtersByReadStateAndCountsUnread() {
    setAuditor(AUDITOR_ID);
    Notification read = notificationRepository.saveAndFlush(notification(RECIPIENT_ID, NotificationType.ACTIVITY_REVIEWED).build());
    read.markRead();
    notificationRepository.saveAndFlush(read);
    notificationRepository.saveAndFlush(notification(RECIPIENT_ID, NotificationType.EVALUATION_SUBMITTED).build());
    notificationRepository.saveAndFlush(notification(OTHER_RECIPIENT_ID, NotificationType.ACTIVITY_REVIEWED).build());

    Page<Notification> unread =
        notificationRepository.findByRecipientIdAndRead(RECIPIENT_ID, false, PageRequest.of(0, 10));
    assertThat(unread.getTotalElements()).isEqualTo(1);

    assertThat(notificationRepository.countByRecipientIdAndRead(RECIPIENT_ID, false)).isEqualTo(1);
    assertThat(notificationRepository.countByRecipientIdAndRead(RECIPIENT_ID, true)).isEqualTo(1);
  }

  private static Notification.Builder notification(UUID recipientId, NotificationType type) {
    return Notification.builder().recipientId(recipientId).type(type).title("Notification");
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