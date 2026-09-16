package com.sitms.ticket;

import java.util.UUID;

/** Payload for updating the state of an existing {@link Ticket}: status and/or assignment. */
public record TicketUpdateRequest(TicketStatus status, TicketAssigneeType assigneeType, UUID assigneeId) {}