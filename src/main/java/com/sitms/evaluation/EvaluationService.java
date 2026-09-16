package com.sitms.evaluation;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.response.Pagination;
import com.sitms.common.security.DataScope;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.SupervisorRepository;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link Evaluation} CRUD (SITMS-041, EPIC 10). */
@Service
public class EvaluationService {

  public static final String EVALUATION_NOT_FOUND = "EVALUATION_NOT_FOUND";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String SUPERVISOR_PROFILE_NOT_FOUND = "SUPERVISOR_PROFILE_NOT_FOUND";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

  private final EvaluationRepository evaluationRepository;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final InternshipRepository internshipRepository;
  private final DataScope dataScope;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;

  public EvaluationService(
      EvaluationRepository evaluationRepository,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      InternshipRepository internshipRepository,
      DataScope dataScope,
      NotificationNotifier notifier,
      AuditLogService auditLogService) {
    this.evaluationRepository = evaluationRepository;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.internshipRepository = internshipRepository;
    this.dataScope = dataScope;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
  }

  /**
   * Create an evaluation. Supervisors evaluate students assigned to them; a scoped supervisor can
   * submit for the students of the supervisor they are currently reviewing (via a picker carry-over),
   * and admins may evaluate any student naming the reviewing supervisor via the request.
   */
  @Transactional
  public EvaluationResponse create(EvaluationRequest request, String role, UUID appUserId) {
    UUID supervisorId =
        "ADMIN".equals(role)
            ? adminSupervisorId(request)
            : (request.supervisorId() != null
                ? request.supervisorId()
                : resolveSupervisorId(appUserId));
    if ("SUPERVISOR".equals(role)
        && !internshipRepository.existsBySupervisorIdAndStudentId(
            supervisorId, request.studentId())) {
      throw new AccessDeniedException(
          "Student is not assigned to this supervisor: " + request.studentId());
    }
    EvaluationStatus status =
        request.status() == null ? EvaluationStatus.SUBMITTED : request.status();
    Evaluation saved =
        evaluationRepository.saveAndFlush(
            Evaluation.builder()
                .studentId(request.studentId())
                .internshipId(request.internshipId())
                .supervisorId(supervisorId)
                .technicalSkills(request.technicalSkills())
                .communication(request.communication())
                .teamwork(request.teamwork())
                .problemSolving(request.problemSolving())
                .professionalism(request.professionalism())
                .strengths(request.strengths())
                .improvement(request.improvement())
                .comments(request.comments())
                .punctuality(request.punctuality())
                .reliability(request.reliability())
                .independenceWork(request.independenceWork())
                .communicationSkills(request.communicationSkills())
                .professionalConduct(request.professionalConduct())
                .learningSpeed(request.learningSpeed())
                .commitment(request.commitment())
                .accuracy(request.accuracy())
                .teamContribution(request.teamContribution())
                .cooperation(request.cooperation())
                .technicalProficiency(request.technicalProficiency())
                .organizationalSkills(request.organizationalSkills())
                .projectSupport(request.projectSupport())
                .taskResponsibility(request.taskResponsibility())
                .qualityTeamMember(request.qualityTeamMember())
                .remarks(request.remarks())
                .department(request.department())
                .companyName(request.companyName())
                .status(status)
                .build());
    if (status == EvaluationStatus.SUBMITTED) {
      notifier.notify(
          studentAppUserId(request.studentId()),
          NotificationType.EVALUATION_SUBMITTED,
          "New evaluation available",
          "A supervisor has submitted an evaluation for your internship.",
          "/evaluations/mine");
      auditLogService.record(
          AuditAction.EVALUATION_SUBMITTED,
          "EVALUATION",
          saved.getId(),
          "Evaluation submitted for student " + request.studentId() + ".");
    }
    return EvaluationResponse.from(saved);
  }

  /** Update an evaluation by its owner supervisor or an admin. A submission cannot be reverted. */
  @Transactional
  public EvaluationResponse update(
      UUID id, EvaluationRequest request, String role, UUID appUserId) {
    Evaluation evaluation = find(id);
    if ("SUPERVISOR".equals(role)) {
      UUID supervisorId =
          request.supervisorId() != null ? request.supervisorId() : resolveSupervisorId(appUserId);
      if (!evaluation.getSupervisorId().equals(supervisorId)) {
        throw new AccessDeniedException("Evaluation is not owned by this supervisor: " + id);
      }
    }
    EvaluationStatus target =
        request.status() == null ? evaluation.getStatus() : request.status();
    if (evaluation.getStatus() == EvaluationStatus.SUBMITTED
        && target == EvaluationStatus.DRAFT) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "A submitted evaluation cannot be reverted to draft: " + id);
    }
    evaluation.update(
        target,
        request.technicalSkills(),
        request.communication(),
        request.teamwork(),
        request.problemSolving(),
        request.professionalism(),
        request.strengths(),
        request.improvement(),
        request.comments(),
        request.punctuality(),
        request.reliability(),
        request.independenceWork(),
        request.communicationSkills(),
        request.professionalConduct(),
        request.learningSpeed(),
        request.commitment(),
        request.accuracy(),
        request.teamContribution(),
        request.cooperation(),
        request.technicalProficiency(),
        request.organizationalSkills(),
        request.projectSupport(),
        request.taskResponsibility(),
        request.qualityTeamMember(),
        request.remarks(),
        request.department(),
        request.companyName());
    return EvaluationResponse.from(evaluationRepository.saveAndFlush(evaluation));
  }

  /** Read one evaluation: students only their own, supervisors only their own, admins any. */
  @Transactional(readOnly = true)
  public EvaluationResponse get(UUID id, String role, UUID appUserId) {
    return get(id, role, appUserId, null, null);
  }

  /** Read one evaluation, with an optional academic staff target for scoped faculty viewers. */
  @Transactional(readOnly = true)
  public EvaluationResponse get(
      UUID id, String role, UUID appUserId, UUID targetSupervisorId, UUID targetAcademicStaffId) {
    Evaluation evaluation = find(id);
    if ("STUDENT".equals(role)) {
      UUID studentId =
          studentRepository
              .findByAppUserId(appUserId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          STUDENT_PROFILE_NOT_FOUND,
                          "No student profile for user: " + appUserId))
              .getId();
      if (!evaluation.getStudentId().equals(studentId)) {
        throw new AccessDeniedException("Evaluation is not accessible to this student: " + id);
      }
    }
    if ("SUPERVISOR".equals(role)) {
      UUID supervisorId =
          targetSupervisorId != null ? targetSupervisorId : resolveSupervisorId(appUserId);
      if (!evaluation.getSupervisorId().equals(supervisorId)) {
        throw new AccessDeniedException("Evaluation is not owned by this supervisor: " + id);
      }
    }
    if ("FACULTY".equals(role)) {
      if (targetAcademicStaffId != null) {
        if (!internshipRepository.existsByAcademicStaffIdAndStudentId(
            targetAcademicStaffId, evaluation.getStudentId())) {
          throw new AccessDeniedException("Evaluation is not accessible to this user: " + id);
        }
      } else if (!dataScope.isAssignedToAcademicStudent(appUserId, evaluation.getStudentId())) {
        throw new AccessDeniedException("Evaluation is not accessible to this user: " + id);
      }
    }
    return EvaluationResponse.from(evaluation);
  }

  /** List evaluations: students see only their own submitted evaluations, supervisors their own. */
  @Transactional(readOnly = true)
  public Page<EvaluationResponse> list(String role, UUID appUserId, Pageable pageable) {
    if ("STUDENT".equals(role)) {
      UUID studentId =
          studentRepository
              .findByAppUserId(appUserId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          STUDENT_PROFILE_NOT_FOUND,
                          "No student profile for user: " + appUserId))
              .getId();
      return evaluationRepository
          .findByStudentIdAndStatus(studentId, EvaluationStatus.SUBMITTED, pageable)
          .map(EvaluationResponse::from);
    }
    if ("SUPERVISOR".equals(role)) {
      UUID supervisorId = resolveSupervisorId(appUserId);
      return evaluationRepository
          .findBySupervisorId(supervisorId, pageable)
          .map(EvaluationResponse::from);
    }
    return evaluationRepository.findAll(pageable).map(EvaluationResponse::from);
  }

  /** Delete an evaluation (admin-only; enforced at the controller). */
  @Transactional
  public void delete(UUID id) {
    Evaluation evaluation = find(id);
    evaluationRepository.delete(evaluation);
  }

  /**
   * The supervisor's assigned students with the state of their evaluations, for the evaluation
   * form (SITMS-042). Admins must name the supervisor via the request parameter.
   */
  @Transactional(readOnly = true)
  public Page<EvaluationAssigneeResponse> listAssignees(
      String role, UUID appUserId, UUID requestedSupervisorId, UUID requestedAcademicStaffId, Pageable pageable) {
    List<Internship> internships;
    if ("SUPERVISOR".equals(role)) {
      UUID targetSupervisorId =
          requestedSupervisorId != null
              ? requestedSupervisorId
              : resolveSupervisorId(appUserId);
      internships =
          internshipRepository
              .findBySupervisorId(targetSupervisorId, Pageable.unpaged())
              .getContent();
    } else if ("FACULTY".equals(role) && requestedAcademicStaffId != null) {
      internships =
          internshipRepository
              .findByAcademicStaffId(requestedAcademicStaffId, Pageable.unpaged())
              .getContent();
    } else if ("FACULTY".equals(role)) {
      internships =
          internshipRepository
              .findByStudentIdIn(dataScope.assignedAcademicStudentIds(appUserId), Pageable.unpaged())
              .getContent();
    } else if (requestedSupervisorId != null) {
      internships =
          internshipRepository
              .findBySupervisorId(requestedSupervisorId, Pageable.unpaged())
              .getContent();
    } else {
      internships = internshipRepository.findAll(Pageable.unpaged()).getContent();
    }
    List<EvaluationAssigneeResponse> all =
        internships.stream()
            .map(
                internship -> {
                  Student student =
                      studentRepository.findById(internship.getStudentId()).orElse(null);
                  Evaluation evaluation =
                      evaluationRepository
                          .findByStudentIdAndInternshipId(
                              internship.getStudentId(), internship.getId())
                          .stream()
                          .findFirst()
                          .orElse(null);
                  return EvaluationAssigneeResponse.from(internship, student, evaluation);
                })
            .sorted(
                Comparator.comparing(
                    EvaluationAssigneeResponse::fullName,
                    Comparator.nullsLast(Comparator.naturalOrder())))
            .toList();
    return Pagination.slice(all, pageable);
  }

  /**
   * The supervisor for an admin-created evaluation: the explicit request value when given,
   * otherwise the supervisor of the evaluation's internship.
   */
  private UUID adminSupervisorId(EvaluationRequest request) {
    if (request.supervisorId() != null) {
      return request.supervisorId();
    }
    if (request.internshipId() != null) {
      return internshipRepository
          .findById(request.internshipId())
          .map(Internship::getSupervisorId)
          .orElseThrow(
              () ->
                  new ResourceNotFoundException(
                      SUPERVISOR_PROFILE_NOT_FOUND,
                      "No internship for evaluation: " + request.internshipId()));
    }
    throw new BusinessException(
        SUPERVISOR_PROFILE_NOT_FOUND,
        "Admins must provide a supervisorId when creating an evaluation.");
  }

  private UUID resolveSupervisorId(UUID appUserId) {
    return supervisorRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    SUPERVISOR_PROFILE_NOT_FOUND,
                    "No supervisor profile for user: " + appUserId))
        .getId();
  }

  /** The app_user id of the student profile, or null. */
  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }

  private Evaluation find(UUID id) {
    return evaluationRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(EVALUATION_NOT_FOUND, "Evaluation not found: " + id));
  }
}