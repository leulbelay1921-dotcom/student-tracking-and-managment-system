package com.sitms.notification;

import com.sitms.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for the notification list/mark-read flows (SITMS-051, EPIC 12).
 *
 * <p>Notifications are addressed to the authenticated user's {@code app_user} row. Students and
 * supervisors are identified directly by their JWT subject (an app_user UUID); the {@code admin}
 * system account subject is a non-UUID string, so its app_user row is looked up by username.
 */
@Service
public class NotificationService {

  public static final String NOTIFICATION_NOT_FOUND = "NOTIFICATION_NOT_FOUND";

  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt");

  private final NotificationRepository notificationRepository;
  private final JdbcTemplate jdbcTemplate;

  public NotificationService(
      NotificationRepository notificationRepository, JdbcTemplate jdbcTemplate) {
    this.notificationRepository = notificationRepository;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** List the current user's notifications, newest first. */
  @Transactional(readOnly = true)
  public Page<NotificationResponse> list(String role, String principalName, Pageable pageable) {
    UUID recipientId = resolveRecipientId(role, principalName);
    if (recipientId == null) {
      return Page.empty();
    }
    Pageable sorted =
        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
    return notificationRepository.findByRecipientId(recipientId, sorted).map(NotificationResponse::from);
  }

  /** Number of unread notifications, for the shell's badge. */
  @Transactional(readOnly = true)
  public long unreadCount(String role, String principalName) {
    UUID recipientId = resolveRecipientId(role, principalName);
    if (recipientId == null) {
      return 0;
    }
    return notificationRepository.countByRecipientIdAndRead(recipientId, false);
  }

  /** Mark one of the caller's notifications as read. Others' notifications resolve to 404. */
  @Transactional
  public NotificationResponse markRead(UUID id, String role, String principalName) {
    UUID recipientId = resolveRecipientId(role, principalName);
    Notification notification = findOwned(id, recipientId);
    if (!notification.isRead()) {
      notification.markRead();
      notificationRepository.save(notification);
    }
    return NotificationResponse.from(notification);
  }

  /** Mark every one of the caller's notifications as read; returns the number read. */
  @Transactional
  public long markAllRead(String role, String principalName) {
    UUID recipientId = resolveRecipientId(role, principalName);
    if (recipientId == null) {
      return 0;
    }
    List<Notification> unread =
        notificationRepository
            .findByRecipientIdAndRead(recipientId, false, Pageable.unpaged())
            .getContent();
    unread.forEach(Notification::markRead);
    return notificationRepository.saveAll(unread).size();
  }

  private Notification findOwned(UUID id, UUID recipientId) {
    Notification notification =
        notificationRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        NOTIFICATION_NOT_FOUND, "Notification not found: " + id));
    if (recipientId == null || !notification.getRecipientId().equals(recipientId)) {
      throw new ResourceNotFoundException(NOTIFICATION_NOT_FOUND, "Notification not found: " + id);
    }
    return notification;
  }

  private UUID resolveRecipientId(String role, String principalName) {
    if (!"ADMIN".equals(role)) {
      return UUID.fromString(principalName);
    }
    List<UUID> matches =
        jdbcTemplate.query(
            "SELECT id FROM app_user WHERE username = ?",
            (rs, rowNum) -> rs.getObject(1, UUID.class),
            principalName);
    if (!matches.isEmpty()) {
      return matches.get(0);
    }
    // The Keycloak admin subject is a UUID that maps 1:1 to app_user.id,
    // so fall back to treating the subject as the recipient id.
    return parseUuid(principalName);
  }

  private static UUID parseUuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}