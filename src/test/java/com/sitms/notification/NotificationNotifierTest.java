package com.sitms.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sitms.common.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test for {@link NotificationNotifier} (SITMS-053): it persists the in-app
 * notification, deals the same payload to the email channel, and skips unwired recipients.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
@ComponentScan(
    basePackageClasses = NotificationNotifier.class,
    useDefaultFilters = false,
    includeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = NotificationNotifier.class))
class NotificationNotifierTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID RECIPIENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID UNKNOWN_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

  @Autowired private NotificationNotifier notifier;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @MockBean private EmailNotificationSender emailSender;

  @BeforeEach
  void seedRecipient() {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, 'recipient1', 'recipient1@example.com', 'Recipient', 'STUDENT', TRUE, ?)",
        RECIPIENT_ID,
        AUDITOR_ID);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persistsInAppNotificationAndEmailsRecipient() {
    setAuditor(AUDITOR_ID);
    notifier.notify(
        RECIPIENT_ID,
        NotificationType.ACTIVITY_REVIEWED,
        "Activity approved",
        "Your log was approved.",
        "/activities");

    org.springframework.data.domain.Pageable pageable =
        org.springframework.data.domain.PageRequest.of(0, 10);
    Notification found =
        notificationRepository
            .findByRecipientId(RECIPIENT_ID, pageable)
            .getContent()
            .get(0);
    assertThat(found.getType()).isEqualTo(NotificationType.ACTIVITY_REVIEWED);
    assertThat(found.getTitle()).isEqualTo("Activity approved");
    assertThat(found.getBody()).isEqualTo("Your log was approved.");
    assertThat(found.getLink()).isEqualTo("/activities");
    assertThat(found.isRead()).isFalse();
    assertThat(notificationRepository.count()).isEqualTo(1);

    verify(emailSender)
        .send(RECIPIENT_ID, NotificationType.ACTIVITY_REVIEWED, "Activity approved", "Your log was approved.");
  }

  @Test
  void skipsRecipientWithoutAppUserRow() {
    setAuditor(AUDITOR_ID);
    notifier.notify(UNKNOWN_ID, NotificationType.ACTIVITY_SUBMITTED, "Title", "Body", null);

    assertThat(notificationRepository.count()).isZero();
    verify(emailSender, never()).send(any(), any(), any(), any());
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
