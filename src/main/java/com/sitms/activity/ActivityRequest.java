package com.sitms.activity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload for creating or updating an {@link Activity}. */
public record ActivityRequest(
    UUID studentId,
    UUID internshipId,
    @NotNull LocalDate activityDate,
    @NotNull ActivityType type,
    @Size(max = 128) String title,
    @NotBlank @Size(max = 2000) String description,
    @Size(max = 2000) String skills,
    @Min(0) @Max(24) Double hours,
    List<UUID> attachmentDocumentIds) {}
