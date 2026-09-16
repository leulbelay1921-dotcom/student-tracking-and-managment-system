package com.sitms.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Payload for creating a student-issued {@link Ticket} (SITMS-073). */
public record TicketRequest(
    @NotNull UUID internshipId,
    @NotBlank @Size(max = 128) String title,
    @NotBlank @Size(max = 4000) String description,
    TicketPriority priority) {

  public TicketPriority priorityOrDefault() {
    return priority == null ? TicketPriority.MEDIUM : priority;
  }
}