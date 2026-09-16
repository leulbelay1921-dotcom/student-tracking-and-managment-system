package com.sitms.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Payload for a student reporting incorrect attendance. */
public record AttendanceIssueRequest(
    @NotNull UUID attendanceId, @NotBlank @Size(max = 2000) String message) {}