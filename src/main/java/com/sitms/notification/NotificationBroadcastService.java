package com.sitms.notification;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notification broadcasting (POST /notifications/broadcast).
 *
 * <p>Broadcasts an {@code ADMIN_BROADCAST} notification to a recipient group: all students, company
 * supervisors, university supervisors, a single student, or everyone tied to one organization (its
 * company supervisors plus students placed there). Recipients are resolved to {@code app_user}
 * accounts; the notifier skips any that no longer exist.
 */
@Service
public class NotificationBroadcastService {

  public static final String RECIPIENT_REQUIRED = "RECIPIENT_REQUIRED";
  public static final String STUDENT_NOT_FOUND = "STUDENT_NOT_FOUND";
  public static final String ORGANIZATION_NOT_FOUND = "ORGANIZATION_NOT_FOUND";

  private final NotificationNotifier notifier;
  private final JdbcTemplate jdbcTemplate;

  public NotificationBroadcastService(NotificationNotifier notifier, JdbcTemplate jdbcTemplate) {
    this.notifier = notifier;
    this.jdbcTemplate = jdbcTemplate;
  }

  /** Resolve the audience to app_user ids and create one notification per recipient. */
  @Transactional
  public BroadcastSummary broadcast(AdminBroadcastRequest request) {
    Set<UUID> recipientIds = resolveRecipients(request.audience(), request.recipientId());
    for (UUID recipientId : recipientIds) {
      notifier.notify(
          recipientId,
          NotificationType.ADMIN_BROADCAST,
          request.title(),
          request.body(),
          request.link());
    }
    return new BroadcastSummary(recipientIds.size());
  }

  private Set<UUID> resolveRecipients(Audience audience, UUID recipientId) {
    return switch (audience) {
      case ALL_STUDENTS -> appUserIds(
          "SELECT s.app_user_id FROM student s JOIN app_user u ON u.id = s.app_user_id"
              + " WHERE u.active = TRUE");
      case COMPANY_SUPERVISORS -> appUserIds(
          "SELECT s.app_user_id FROM supervisor s JOIN app_user u ON u.id = s.app_user_id"
              + " WHERE s.active = TRUE AND u.active = TRUE");
      case UNIVERSITY_SUPERVISORS -> appUserIds(
          "SELECT s.app_user_id FROM academic_staff s JOIN app_user u ON u.id = s.app_user_id"
              + " WHERE s.active = TRUE AND u.active = TRUE");
      case STUDENT -> studentRecipient(recipientId);
      case ORGANIZATION -> organizationRecipients(recipientId);
    };
  }

  private Set<UUID> studentRecipient(UUID recipientId) {
    requireRecipient(recipientId);
    Set<UUID> ids =
        appUserIds("SELECT app_user_id FROM student WHERE id = ?", recipientId);
    if (ids.isEmpty()) {
      throw new ResourceNotFoundException(STUDENT_NOT_FOUND, "Student not found: " + recipientId);
    }
    return ids;
  }

  private Set<UUID> organizationRecipients(UUID recipientId) {
    requireRecipient(recipientId);
    List<UUID> orgs =
        jdbcTemplate.query(
            "SELECT id FROM organization WHERE id = ?",
            (rs, rowNum) -> rs.getObject(1, UUID.class),
            recipientId);
    if (orgs.isEmpty()) {
      throw new ResourceNotFoundException(
          ORGANIZATION_NOT_FOUND, "Organization not found: " + recipientId);
    }
    Set<UUID> ids = new LinkedHashSet<>();
    ids.addAll(
        appUserIds(
            "SELECT s.app_user_id FROM supervisor s JOIN app_user u ON u.id = s.app_user_id"
                + " WHERE s.organization_id = ? AND s.active = TRUE AND u.active = TRUE",
            recipientId));
    ids.addAll(
        appUserIds(
            "SELECT s.app_user_id FROM student s"
                + " JOIN internship i ON i.student_id = s.id"
                + " JOIN app_user u ON u.id = s.app_user_id"
                + " WHERE i.organization_id = ? AND u.active = TRUE",
            recipientId));
    return ids;
  }

  private static void requireRecipient(UUID recipientId) {
    if (recipientId == null) {
      throw new BusinessException(
          RECIPIENT_REQUIRED, "A recipient is required for this audience.");
    }
  }

  private Set<UUID> appUserIds(String sql, Object... args) {
    return new LinkedHashSet<>(
        jdbcTemplate.query(sql, (rs, rowNum) -> rs.getObject(1, UUID.class), args));
  }
}