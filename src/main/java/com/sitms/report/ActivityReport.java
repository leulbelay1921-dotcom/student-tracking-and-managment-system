package com.sitms.report;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Activity report (SITMS-046).
 *
 * <p>Volume, status distribution, approval rate (approved over non-draft activities), pending
 * review backlog, and logged hours. {@code studentId} is {@code null} when the report spans every
 * student in the caller's scope (all students for admins, assigned students for supervisors).
 */
public record ActivityReport(
    ReportType type,
    OffsetDateTime generatedAt,
    UUID studentId,
    long totalActivities,
    Map<String, Long> statusCounts,
    double approvalRate,
    long pendingReviews,
    Double totalHours) {}