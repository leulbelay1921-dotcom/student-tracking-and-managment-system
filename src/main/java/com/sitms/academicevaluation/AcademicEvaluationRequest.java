package com.sitms.academicevaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Payload for creating or updating an {@link AcademicEvaluation}.
 *
 * <p>All six dimension scores are required (1-5): Academic Learning, Problem Solving, Documentation,
 * Technical Development, Professionalism and Final Presentation. {@code evaluatorId} is only
 * honoured when an ADMIN saves an evaluation (admins designate the reviewing academic staff member);
 * academic supervisors are forced to their own profile. The evaluation is keyed by
 * {@code internshipId} — one record per internship — so saving a second time for the same
 * internship updates the existing record.
 */
public record AcademicEvaluationRequest(
    @NotNull UUID internshipId,
    @NotNull UUID studentId,
    @Min(1) @Max(5) @NotNull Integer academicLearning,
    @Min(1) @Max(5) @NotNull Integer problemSolving,
    @Min(1) @Max(5) @NotNull Integer documentation,
    @Min(1) @Max(5) @NotNull Integer technicalDevelopment,
    @Min(1) @Max(5) @NotNull Integer professionalism,
    @Min(1) @Max(5) @NotNull Integer finalPresentation,
    @Size(max = 2000) String remarks,
    UUID evaluatorId) {}