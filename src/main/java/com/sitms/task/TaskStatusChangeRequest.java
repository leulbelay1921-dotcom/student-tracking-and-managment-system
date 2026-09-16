package com.sitms.task;

import jakarta.validation.constraints.NotNull;

/** Payload for transitioning a {@link Task} to a new {@link TaskStatus}. */
public record TaskStatusChangeRequest(@NotNull TaskStatus status) {}
