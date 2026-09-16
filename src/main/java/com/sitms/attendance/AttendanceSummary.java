package com.sitms.attendance;

import java.util.UUID;

/**
 * Aggregated attendance for a student (SITMS-070).
 *
 * <p>{@code totalDays} counts recorded rows only: PRESENT + LATE + ABSENT + PERMISSION days.
 * {@code rate} is the percentage of attending days (PRESENT + LATE + PERMISSION) over recorded
 * days (rounded to 0.1), {@code null} when no records exist.
 */
public record AttendanceSummary(
    UUID studentId,
    long totalDays,
    long presentDays,
    long lateDays,
    long absentDays,
    long permissionDays,
    Double rate) {}