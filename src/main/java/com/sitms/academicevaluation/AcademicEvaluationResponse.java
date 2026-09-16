package com.sitms.academicevaluation;

import java.time.OffsetDateTime;
import java.util.UUID;

/** API view of an {@link AcademicEvaluation}. */
public record AcademicEvaluationResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    UUID evaluatorId,
    int academicLearning,
    int problemSolving,
    int documentation,
    int technicalDevelopment,
    int professionalism,
    int finalPresentation,
    int totalScore,
    String remarks,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static AcademicEvaluationResponse from(AcademicEvaluation evaluation) {
    return new AcademicEvaluationResponse(
        evaluation.getId(),
        evaluation.getStudentId(),
        evaluation.getInternshipId(),
        evaluation.getEvaluatorId(),
        evaluation.getAcademicLearning(),
        evaluation.getProblemSolving(),
        evaluation.getDocumentation(),
        evaluation.getTechnicalDevelopment(),
        evaluation.getProfessionalism(),
        evaluation.getFinalPresentation(),
        evaluation.getTotalScore(),
        evaluation.getRemarks(),
        evaluation.getCreatedAt(),
        evaluation.getUpdatedAt());
  }
}