package com.sitms.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Payload for a supervisor correcting an attendance record: the student and date identify (or
 * create) the row, and each provided column overwrites it while absent ones are kept (SITMS-070).
 */
public record AttendanceCorrectionRequest(
    @NotNull UUID studentId,
    @NotNull LocalDate date,
    AttendanceStatus status,
    LocalTime checkIn,
    LocalTime checkOut,
    @Size(max = 500, message = "Note must be at most 500 characters.") String note) {}