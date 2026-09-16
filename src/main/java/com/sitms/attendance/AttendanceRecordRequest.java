package com.sitms.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Payload for a supervisor recording a student's daily attendance. */
public record AttendanceRecordRequest(
    @NotNull UUID studentId,
    @NotNull AttendanceStatus status,
    LocalDate date,
    @Size(max = 500) String note) {}