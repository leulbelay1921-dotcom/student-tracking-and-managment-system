package com.sitms.application;

import jakarta.validation.constraints.NotNull;

/** Payload for deciding a pending {@link Application} (approve or reject). */
public record ApplicationStatusChangeRequest(@NotNull ApplicationStatus status) {}