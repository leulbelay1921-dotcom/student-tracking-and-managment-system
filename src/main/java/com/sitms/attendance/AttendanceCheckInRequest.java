package com.sitms.attendance;

import java.time.LocalDate;
import java.util.UUID;

/** Payload for a student self check-in ({@code date} defaults to today). */
public record AttendanceCheckInRequest(UUID internshipId, LocalDate date) {}