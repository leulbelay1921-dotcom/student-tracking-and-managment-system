package com.sitms.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Aggregate analytics over students, internships and activities (SITMS-035).
 *
 * <p>{@code activityTrends} buckets non-draft activities by the Monday of their activity week,
 * reporting how many entered review (submitted) and how many were approved.
 */
public record AnalyticsSummary(
    long totalStudents,
    Map<String, Long> studentsByProgram,
    Map<String, Long> internshipsByStatus,
    List<TrendPoint> activityTrends) {

  public record TrendPoint(LocalDate week, long submitted, long approved) {}
}
