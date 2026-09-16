package com.sitms.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Payload for a student submitting an internship application. */
public record ApplicationRequest(
    @NotNull UUID organizationId,
    @NotBlank @Size(max = 128) String title,
    @Size(max = 2000) String description,
    LocalDate startDate,
    LocalDate endDate) {}