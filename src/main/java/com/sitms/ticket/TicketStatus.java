package com.sitms.ticket;

/**
 * Lifecycle of a {@link Ticket} (SITMS-073): a student-issued ticket starts {@code OPEN}, is
 * worked ({@code IN_PROGRESS}) by an assigned supervisor or faculty member, and ends as
 * {@code RESOLVED} or {@code CLOSED}. Values are persisted as strings and must not be renamed.
 */
public enum TicketStatus {
  OPEN,
  IN_PROGRESS,
  RESOLVED,
  CLOSED
}