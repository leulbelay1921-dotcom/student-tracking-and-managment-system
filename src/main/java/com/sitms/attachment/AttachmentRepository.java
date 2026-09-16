package com.sitms.attachment;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Attachment}. */
public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {

  List<Attachment> findByOwnerTypeAndOwnerId(AttachmentOwnerType ownerType, UUID ownerId);

  List<Attachment> findByOwnerTypeAndOwnerIdIn(
      AttachmentOwnerType ownerType, Collection<UUID> ownerIds);

  void deleteByOwnerTypeAndOwnerId(AttachmentOwnerType ownerType, UUID ownerId);
}
