package com.sitms.ticket;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Ticket} (SITMS-073). */
public interface TicketRepository
    extends JpaRepository<Ticket, UUID>, JpaSpecificationExecutor<Ticket> {

  Page<Ticket> findByStudentId(UUID studentId, Pageable pageable);

  Page<Ticket> findByInternshipId(UUID internshipId, Pageable pageable);

  Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);
}