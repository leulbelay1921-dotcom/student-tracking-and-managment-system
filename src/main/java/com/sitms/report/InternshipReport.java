package com.sitms.report;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Internship summary report (SITMS-045).
 *
 * <p>Placement volume, status distribution, average duration, and per-program placement rates,
 * computed over live data at generation time. The placement rate is the share of internships
 * with an admin decision (non-{@code PENDING}) that resulted in a placement ({@code APPROVED} or
 * {@code COMPLETED}).
 */
public record InternshipReport(
    ReportType type,
    OffsetDateTime generatedAt,
    long totalInternships,
    Map<String, Long> statusCounts,
    double placementRate,
    Double avgDurationDays,
    List<ProgramStat> byProgram) {

  public record ProgramStat(String program, long internships, double placementRate) {}
}