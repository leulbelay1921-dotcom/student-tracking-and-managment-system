package com.sitms.academicevaluation;

import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.response.Pagination;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link AcademicEvaluation} (SITMS-078).
 *
 * <p>An academic evaluation is keyed to a single internship: saving for an internship that already
 * has an evaluation updates it. Academic supervisors (FACULTY) may only evaluate internships
 * assigned to their own academic staff profile; admins may evaluate any internship and must either
 * name the evaluator explicitly or rely on the internship's assigned academic staff.
 */
@Service
public class AcademicEvaluationService {

  public static final String ACADEMIC_EVALUATION_NOT_FOUND = "ACADEMIC_EVALUATION_NOT_FOUND";
  public static final String ACADEMIC_STAFF_PROFILE_NOT_FOUND = "ACADEMIC_STAFF_PROFILE_NOT_FOUND";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String INTERNSHIP_NOT_FOUND = "INTERNSHIP_NOT_FOUND";
  public static final String INTERNSHIP_ACADEMIC_STAFF_REQUIRED =
      "INTERNSHIP_ACADEMIC_STAFF_REQUIRED";
  public static final String INTERNSHIP_NOT_ASSIGNED = "INTERNSHIP_NOT_ASSIGNED";

  private final AcademicEvaluationRepository academicEvaluationRepository;
  private final InternshipRepository internshipRepository;
  private final StudentRepository studentRepository;
  private final AcademicStaffRepository academicStaffRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;

  public AcademicEvaluationService(
      AcademicEvaluationRepository academicEvaluationRepository,
      InternshipRepository internshipRepository,
      StudentRepository studentRepository,
      AcademicStaffRepository academicStaffRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService) {
    this.academicEvaluationRepository = academicEvaluationRepository;
    this.internshipRepository = internshipRepository;
    this.studentRepository = studentRepository;
    this.academicStaffRepository = academicStaffRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
  }

  /**
   * Create or update the academic evaluation for an internship. Academic supervisors evaluate
   * internships assigned to them; admins may evaluate any internship and must name the reviewing
   * academic staff member (or rely on the internship's assigned academic staff).
   */
  @Transactional
  public AcademicEvaluationResponse upsert(
      AcademicEvaluationRequest request, String role, UUID appUserId) {
    Internship internship =
        internshipRepository
            .findById(request.internshipId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        INTERNSHIP_NOT_FOUND,
                        "No internship for academic evaluation: " + request.internshipId()));
    UUID evaluatorId =
        "ADMIN".equals(role)
            ? adminEvaluatorId(request, internship)
            : resolveAcademicStaffId(appUserId);
    if ("FACULTY".equals(role)
        && (internship.getAcademicStaffId() == null
            || !internship.getAcademicStaffId().equals(evaluatorId))) {
      throw new AccessDeniedException(
          "Internship is not assigned to this academic supervisor: " + request.internshipId());
    }

    return academicEvaluationRepository
        .findByInternshipId(internship.getId())
        .map(
            existing -> {
              existing.update(
                  request.academicLearning(),
                  request.problemSolving(),
                  request.documentation(),
                  request.technicalDevelopment(),
                  request.professionalism(),
                  request.finalPresentation(),
                  request.remarks());
              return AcademicEvaluationResponse.from(
                  academicEvaluationRepository.saveAndFlush(existing));
            })
        .orElseGet(
            () -> {
              AcademicEvaluation saved =
                  academicEvaluationRepository.saveAndFlush(
                      AcademicEvaluation.builder()
                          .studentId(request.studentId())
                          .internshipId(internship.getId())
                          .evaluatorId(evaluatorId)
                          .academicLearning(request.academicLearning())
                          .problemSolving(request.problemSolving())
                          .documentation(request.documentation())
                          .technicalDevelopment(request.technicalDevelopment())
                          .professionalism(request.professionalism())
                          .finalPresentation(request.finalPresentation())
                          .remarks(request.remarks())
                          .build());
              notifier.notify(
                  studentAppUserId(request.studentId()),
                  NotificationType.ACADEMIC_EVALUATION_SUBMITTED,
                  "Academic evaluation submitted",
                  "Your academic supervisor has submitted an evaluation for your internship.",
                  "/academic-evaluations/mine");
              auditLogService.record(
                  AuditAction.ACADEMIC_EVALUATION_SUBMITTED,
                  "ACADEMIC_EVALUATION",
                  saved.getId(),
                  "Academic evaluation submitted for internship "
                      + internship.getId()
                      + ".");
              return AcademicEvaluationResponse.from(saved);
            });
  }

  /** Read one academic evaluation: students only their own, academic supervisors their own, admins any. */
  @Transactional(readOnly = true)
  public AcademicEvaluationResponse get(UUID id, String role, UUID appUserId) {
    AcademicEvaluation evaluation = find(id);
    if ("STUDENT".equals(role)) {
      UUID studentId = studentId(appUserId);
      if (!evaluation.getStudentId().equals(studentId)) {
        throw new AccessDeniedException(
            "Academic evaluation is not accessible to this student: " + id);
      }
    }
    if ("FACULTY".equals(role)) {
      UUID evaluatorId = resolveAcademicStaffId(appUserId);
      if (!evaluation.getEvaluatorId().equals(evaluatorId)) {
        throw new AccessDeniedException(
            "Academic evaluation is not owned by this academic supervisor: " + id);
      }
    }
    return AcademicEvaluationResponse.from(evaluation);
  }

  /** The academic evaluation for one internship, or null when none exists yet. */
  @Transactional(readOnly = true)
  public AcademicEvaluationResponse byInternship(
      UUID internshipId, String role, UUID appUserId) {
    Internship internship =
        internshipRepository
            .findById(internshipId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        INTERNSHIP_NOT_FOUND, "No internship found: " + internshipId));
    if ("STUDENT".equals(role)) {
      if (!internship.getStudentId().equals(studentId(appUserId))) {
        throw new AccessDeniedException(
            "Internship is not accessible to this student: " + internshipId);
      }
    }
    if ("FACULTY".equals(role)
        && (internship.getAcademicStaffId() == null
            || !internship.getAcademicStaffId().equals(resolveAcademicStaffId(appUserId)))) {
      throw new AccessDeniedException(
          "Internship is not assigned to this academic supervisor: " + internshipId);
    }
    return academicEvaluationRepository
        .findByInternshipId(internshipId)
        .map(AcademicEvaluationResponse::from)
        .orElse(null);
  }

  /** List academic evaluations: students only their own, academic supervisors their own, admins any. */
  @Transactional(readOnly = true)
  public Page<AcademicEvaluationResponse> list(
      String role, UUID appUserId, Pageable pageable) {
    if ("STUDENT".equals(role)) {
      UUID studentId = studentId(appUserId);
      return academicEvaluationRepository
          .findByStudentId(studentId, pageable)
          .map(AcademicEvaluationResponse::from);
    }
    if ("FACULTY".equals(role)) {
      UUID evaluatorId = resolveAcademicStaffId(appUserId);
      return academicEvaluationRepository
          .findByEvaluatorId(evaluatorId, pageable)
          .map(AcademicEvaluationResponse::from);
    }
    return academicEvaluationRepository.findAll(pageable).map(AcademicEvaluationResponse::from);
  }

  /**
   * The academic supervisor's assigned students with the state of their academic evaluations, for
   * the evaluation form. Admins must name the academic staff member via the request parameter.
   */
  @Transactional(readOnly = true)
  public Page<AcademicEvaluationAssigneeResponse> listAssignees(
      String role, UUID appUserId, UUID requestedAcademicStaffId, Pageable pageable) {
    List<Internship> internships;
    if ("FACULTY".equals(role)) {
      internships =
          internshipRepository
              .findByAcademicStaffId(resolveAcademicStaffId(appUserId), Pageable.unpaged())
              .getContent();
    } else if (requestedAcademicStaffId != null) {
      internships =
          internshipRepository
              .findByAcademicStaffId(requestedAcademicStaffId, Pageable.unpaged())
              .getContent();
    } else {
      internships = internshipRepository.findAll(Pageable.unpaged()).getContent();
    }
    List<AcademicEvaluationAssigneeResponse> all =
        internships.stream()
            .map(
                internship -> {
                  Student student =
                      studentRepository.findById(internship.getStudentId()).orElse(null);
                  AcademicEvaluation evaluation =
                      academicEvaluationRepository
                          .findByInternshipId(internship.getId())
                          .orElse(null);
                  return AcademicEvaluationAssigneeResponse.from(internship, student, evaluation);
                })
            .sorted(
                Comparator.comparing(
                    AcademicEvaluationAssigneeResponse::fullName,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    return Pagination.slice(all, pageable);
  }

  /**
   * The evaluator for an admin-saved academic evaluation: the explicit request value when given,
   * otherwise the academic staff assigned to the internship.
   */
  private UUID adminEvaluatorId(
      AcademicEvaluationRequest request, Internship internship) {
    if (request.evaluatorId() != null) {
      return request.evaluatorId();
    }
    if (internship.getAcademicStaffId() != null) {
      return internship.getAcademicStaffId();
    }
    throw new BusinessException(
        INTERNSHIP_ACADEMIC_STAFF_REQUIRED,
        "No academic staff assigned to internship: " + internship.getId());
  }

  private AcademicEvaluationRequest withEvaluator(
      AcademicEvaluationRequest request, UUID studentId) {
    return request;
  }

  private UUID resolveAcademicStaffId(UUID appUserId) {
    return academicStaffRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ACADEMIC_STAFF_PROFILE_NOT_FOUND,
                    "No academic staff profile for user: " + appUserId))
        .getId();
  }

  private UUID studentId(UUID appUserId) {
    return studentRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    STUDENT_PROFILE_NOT_FOUND,
                    "No student profile for user: " + appUserId))
        .getId();
  }

  /** The app_user id of the student profile, or null. */
  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }

  private AcademicEvaluation find(UUID id) {
    return academicEvaluationRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ACADEMIC_EVALUATION_NOT_FOUND,
                    "Academic evaluation not found: " + id));
  }
}