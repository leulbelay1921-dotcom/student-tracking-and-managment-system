package com.sitms.notification;

import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Produces an in-app notification and optionally an email (SITMS-053, EPIC 12).
 *
 * <p>Domain services call {@link #notify(UUID, NotificationType, String, String, String)} at their
 * event boundaries; the notifier persists the in-app row (SITMS-050/051) and hands it to the email
 * channel. Notifications are addressed by {@code app_user} id; when no app_user row exists for the
 * recipient the whole notification is skipped so a missing account can never break the domain
 * operation.
 */
@Component
public class NotificationNotifier {

  private final NotificationRepository notificationRepository;
  private final EmailNotificationSender emailSender;
  private final JdbcTemplate jdbcTemplate;

  public NotificationNotifier(
      NotificationRepository notificationRepository,
      EmailNotificationSender emailSender,
      JdbcTemplate jdbcTemplate) {
    this.notificationRepository = notificationRepository;
    this.emailSender = emailSender;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public void notify(
      UUID recipientId, NotificationType type, String title, String body, String link) {
    if (recipientId == null || !appUserExists(recipientId)) {
      return;
    }
    notificationRepository.save(
        Notification.builder()
            .recipientId(recipientId)
            .type(type)
            .title(title)
            .body(body)
            .link(link)
            .build());
    emailSender.send(recipientId, type, title, body);
  }

  /** Notifies every active admin account (the {@code ADMIN} role in {@code app_user}). */
  @Transactional
  public void notifyAdmins(NotificationType type, String title, String body, String link) {
    List<UUID> adminIds =
        jdbcTemplate.query(
            "SELECT id FROM app_user WHERE role = ? AND active = TRUE",
            (rs, rowNum) -> rs.getObject(1, UUID.class),
            "ADMIN");
    adminIds.forEach(id -> notify(id, type, title, body, link));
  }

  private boolean appUserExists(UUID recipientId) {
    List<Integer> matches =
        jdbcTemplate.query(
            "SELECT 1 FROM app_user WHERE id = ?", (rs, rowNum) -> rs.getInt(1), recipientId);
    return !matches.isEmpty();
  }
}