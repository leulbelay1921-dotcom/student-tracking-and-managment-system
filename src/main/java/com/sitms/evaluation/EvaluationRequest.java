package com.sitms.evaluation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Payload for creating or updating an {@link Evaluation}.
 *
 * <p>All five dimension scores are required (1-5). {@code supervisorId} is only honoured when an
 * ADMIN creates an evaluation (admins designate the reviewing supervisor); supervisors are forced
 * to their own profile. {@code status} defaults to {@code SUBMITTED} on create and is preserved on
 * update when omitted. Paper-criteria scores are optional (0-5 each, with {@code taskResponsibility}
 * 0-15 and {@code qualityTeamMember} 0-20) and when all are present {@code overallScore} is computed
 * as their sum.
 */
public record EvaluationRequest(
    @NotNull UUID studentId,
    @Min(1) @Max(5) @NotNull Integer technicalSkills,
    @Min(1) @Max(5) @NotNull Integer communication,
    @Min(1) @Max(5) @NotNull Integer teamwork,
    @Min(1) @Max(5) @NotNull Integer problemSolving,
    @Min(1) @Max(5) @NotNull Integer professionalism,
    @Size(max = 2000) String strengths,
    @Size(max = 2000) String improvement,
    @Size(max = 2000) String comments,
    UUID internshipId,
    UUID supervisorId,
    EvaluationStatus status,
    @Min(0) @Max(5) Integer punctuality,
    @Min(0) @Max(5) Integer reliability,
    @Min(0) @Max(5) Integer independenceWork,
    @Min(0) @Max(5) Integer communicationSkills,
    @Min(0) @Max(5) Integer professionalConduct,
    @Min(0) @Max(5) Integer learningSpeed,
    @Min(0) @Max(5) Integer commitment,
    @Min(0) @Max(5) Integer accuracy,
    @Min(0) @Max(5) Integer teamContribution,
    @Min(0) @Max(5) Integer cooperation,
    @Min(0) @Max(5) Integer technicalProficiency,
    @Min(0) @Max(5) Integer organizationalSkills,
    @Min(0) @Max(5) Integer projectSupport,
    @Min(0) @Max(15) Integer taskResponsibility,
    @Min(0) @Max(20) Integer qualityTeamMember,
    @Size(max = 2000) String remarks,
    @Size(max = 128) String department,
    @Size(max = 255) String companyName) {}