package com.sitms.activity;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for a supervisor's review of an {@link Activity} (SITMS-028). */
public record ActivityReviewRequest(
    @NotNull ReviewDecision decision,
    @Size(max = 2000) String feedback) {}
