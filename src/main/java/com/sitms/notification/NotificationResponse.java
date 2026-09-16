package com.sitms.notification;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link Notification}. */
public record NotificationResponse(
    UUID id,
    NotificationType type,
    String title,
    String body,
    String link,
    boolean read,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getId(),
        notification.getType(),
        notification.getTitle(),
        notification.getBody(),
        notification.getLink(),
        notification.isRead(),
        notification.getCreatedAt(),
        notification.getUpdatedAt());
  }
}