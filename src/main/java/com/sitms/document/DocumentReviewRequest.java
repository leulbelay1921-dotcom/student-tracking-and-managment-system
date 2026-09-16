package com.sitms.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Payload for a reviewer's decision on a {@link Document} (SITMS-039). */
public record DocumentReviewRequest(
    @NotNull DocumentStatus decision,
    @Size(max = 2000) String feedback) {}