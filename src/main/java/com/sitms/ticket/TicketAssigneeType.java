package com.sitms.ticket;

/**
 * Kind of user a {@link Ticket} is assigned to (SITMS-073): a company workplace supervisor or a
 * university academic staff member. Values are persisted as strings and must not be renamed.
 */
public enum TicketAssigneeType {
  SUPERVISOR,
  FACULTY
}