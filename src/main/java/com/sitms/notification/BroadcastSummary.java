package com.sitms.notification;

/** Result of an admin broadcast: how many app_user accounts received it. */
public record BroadcastSummary(long recipients) {}