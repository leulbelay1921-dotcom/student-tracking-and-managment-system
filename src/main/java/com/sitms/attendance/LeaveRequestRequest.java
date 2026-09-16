package com.sitms.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Payload for a student submitting a leave request. */
public record LeaveRequestRequest(
    UUID internshipId,
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank @Size(max = 2000) String reason) {}