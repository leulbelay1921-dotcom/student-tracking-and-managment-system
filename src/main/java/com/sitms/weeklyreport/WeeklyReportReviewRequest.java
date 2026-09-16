package com.sitms.weeklyreport;

import jakarta.validation.constraints.NotNull;

/**
 * A single-stage decision on a weekly report; {@code approved} selects the next state. Setting
 * {@code requestCorrection} at the academic stage sends the report back for re-work without
 * rejecting it (SITMS-077).
 */
public record WeeklyReportReviewRequest(
    @NotNull(message = "A decision is required.") Boolean approved, String comment, Boolean requestCorrection) {}