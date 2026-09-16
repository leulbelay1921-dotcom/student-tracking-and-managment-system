package com.sitms.academicevaluation;

import com.sitms.internship.Internship;
import com.sitms.student.Student;
import java.util.UUID;

/**
 * An academic supervisor's assigned student plus the state of any existing academic evaluation for
 * that student's internship. Powers the academic evaluation form: pick an assignee, then save the
 * six-dimension review. {@code evaluationId}/{@code totalScore} are null until the academic
 * supervisor has submitted an evaluation.
 */
public record AcademicEvaluationAssigneeResponse(
    UUID internshipId,
    UUID studentId,
    String studentNo,
    String fullName,
    String program,
    UUID evaluationId,
    Integer totalScore) {

  static AcademicEvaluationAssigneeResponse from(
      Internship internship, Student student, AcademicEvaluation evaluation) {
    return new AcademicEvaluationAssigneeResponse(
        internship.getId(),
        internship.getStudentId(),
        student == null ? null : student.getStudentNo(),
        student == null ? null : student.getFullName(),
        student == null ? null : student.getProgram(),
        evaluation == null ? null : evaluation.getId(),
        evaluation == null ? null : evaluation.getTotalScore());
  }
}