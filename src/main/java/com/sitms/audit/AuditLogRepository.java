package com.sitms.audit;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link AuditLog} (SITMS-056). Newest-first reads back the admin console. */
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);
}