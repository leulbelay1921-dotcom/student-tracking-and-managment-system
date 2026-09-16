package com.sitms.notification;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Notification} (SITMS-050). Recipient-scoped reads are the foundation for
 * the list/poll/mark-read flows of SITMS-051.
 */
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Page<Notification> findByRecipientId(UUID recipientId, Pageable pageable);

  Page<Notification> findByRecipientIdAndRead(UUID recipientId, boolean read, Pageable pageable);

  long countByRecipientIdAndRead(UUID recipientId, boolean read);
}