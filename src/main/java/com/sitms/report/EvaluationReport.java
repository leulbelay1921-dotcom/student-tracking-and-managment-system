package com.sitms.report;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Evaluation report (SITMS-047).
 *
 * <p>Submission volume, average dimension and overall scores, and the distribution of overall
 * scores, computed over submitted evaluations only — drafts are excluded because they are not a
 * finished assessment.
 */
public record EvaluationReport(
    ReportType type,
    OffsetDateTime generatedAt,
    long totalEvaluations,
    long submittedEvaluations,
    double submissionRate,
    Map<String, Double> dimensionAverages,
    Double averageOverallScore,
    Map<String, Long> scoreDistribution) {}