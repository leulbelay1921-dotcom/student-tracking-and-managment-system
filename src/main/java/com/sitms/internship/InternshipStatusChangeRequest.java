package com.sitms.internship;

import jakarta.validation.constraints.NotNull;

/** Payload for transitioning an {@link Internship} to a new {@link InternshipStatus}. */
public record InternshipStatusChangeRequest(@NotNull InternshipStatus status) {}
