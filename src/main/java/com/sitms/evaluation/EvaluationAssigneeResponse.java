package com.sitms.evaluation;

import com.sitms.internship.Internship;
import com.sitms.student.Student;
import java.util.UUID;

/**
 * A supervisor's assigned student plus the state of any existing evaluation for that student's
 * internship. Powers the supervisor evaluation form (SITMS-042): pick an assignee, then create or
 * update the evaluation. {@code evaluationId}/{@code evaluationStatus} are null until the
 * supervisor has submitted an evaluation.
 */
public record EvaluationAssigneeResponse(
    UUID internshipId,
    UUID studentId,
    String studentNo,
    String fullName,
    String program,
    UUID evaluationId,
    EvaluationStatus evaluationStatus) {

  static EvaluationAssigneeResponse from(
      Internship internship, Student student, Evaluation evaluation) {
    return new EvaluationAssigneeResponse(
        internship.getId(),
        internship.getStudentId(),
        student == null ? null : student.getStudentNo(),
        student == null ? null : student.getFullName(),
        student == null ? null : student.getProgram(),
        evaluation == null ? null : evaluation.getId(),
        evaluation == null ? null : evaluation.getStatus());
  }
}