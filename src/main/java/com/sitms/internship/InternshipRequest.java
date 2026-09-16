package com.sitms.internship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Payload for creating or updating an {@link Internship}. */
public record InternshipRequest(
    @NotNull UUID studentId,
    @NotNull UUID organizationId,
    UUID supervisorId,
    UUID academicStaffId,
    @NotBlank @Size(max = 128) String title,
    @Size(max = 2000) String description,
    LocalDate startDate,
    LocalDate endDate) {}
