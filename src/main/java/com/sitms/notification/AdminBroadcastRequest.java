package com.sitms.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Payload for a notification broadcast (POST /notifications/broadcast).
 *
 * <p>{@code audience} selects the recipient group; {@code recipientId} is the entity id (student or
 * organization) required by the {@code STUDENT} / {@code ORGANIZATION} audiences. {@code title},
 * {@code body} and {@code link} are the message itself — title is required and capped at the
 * {@code notification.title} column width (255).
 */
public record AdminBroadcastRequest(
    @NotNull Audience audience,
    UUID recipientId,
    @NotBlank @Size(max = 255) String title,
    @Size(max = 2000) String body,
    @Size(max = 255) String link) {}