package com.sitms.ticket;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of a {@link Ticket} (SITMS-073). */
public record TicketResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    String title,
    String description,
    TicketPriority priority,
    TicketStatus status,
    TicketAssigneeType assigneeType,
    UUID assigneeId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static TicketResponse from(Ticket ticket) {
    return new TicketResponse(
        ticket.getId(),
        ticket.getStudentId(),
        ticket.getInternshipId(),
        ticket.getTitle(),
        ticket.getDescription(),
        ticket.getPriority(),
        ticket.getStatus(),
        ticket.getAssigneeType(),
        ticket.getAssigneeId(),
        ticket.getCreatedAt(),
        ticket.getUpdatedAt());
  }
}