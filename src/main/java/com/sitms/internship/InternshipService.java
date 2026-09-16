package com.sitms.internship;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.evaluation.EvaluationRepository;
import com.sitms.evaluation.EvaluationStatus;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.academicstaff.AcademicStaff;
import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.organization.Organization;
import com.sitms.organization.OrganizationRepository;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.Supervisor;
import com.sitms.supervisor.SupervisorRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link Internship} and its status lifecycle (SITMS-019, SITMS-020). */
@Service
public class InternshipService {

  public static final String INTERNSHIP_NOT_FOUND = "INTERNSHIP_NOT_FOUND";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  public static final String EVALUATION_REQUIRED_TO_COMPLETE = "EVALUATION_REQUIRED_TO_COMPLETE";
  public static final String SUPERVISOR_ORGANIZATION_MISMATCH = "SUPERVISOR_ORGANIZATION_MISMATCH";
  public static final String SUPERVISOR_INACTIVE = "SUPERVISOR_INACTIVE";
  public static final String ACADEMIC_STAFF_INACTIVE = "ACADEMIC_STAFF_INACTIVE";
  public static final String STUDENT_ALREADY_ASSIGNED_COMPANY_SUPERVISOR =
      "STUDENT_ALREADY_ASSIGNED_COMPANY_SUPERVISOR";
  public static final String STUDENT_ALREADY_ASSIGNED_UNIVERSITY_SUPERVISOR =
      "STUDENT_ALREADY_ASSIGNED_UNIVERSITY_SUPERVISOR";

  private final InternshipRepository internshipRepository;
  private final StudentRepository studentRepository;
  private final EvaluationRepository evaluationRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;
  private final DataScope dataScope;
  private final SupervisorRepository supervisorRepository;
  private final AcademicStaffRepository academicStaffRepository;
  private final OrganizationRepository organizationRepository;

  public InternshipService(
      InternshipRepository internshipRepository,
      StudentRepository studentRepository,
      EvaluationRepository evaluationRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService,
      DataScope dataScope,
      SupervisorRepository supervisorRepository,
      AcademicStaffRepository academicStaffRepository,
      OrganizationRepository organizationRepository) {
    this.internshipRepository = internshipRepository;
    this.studentRepository = studentRepository;
    this.evaluationRepository = evaluationRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
    this.dataScope = dataScope;
    this.supervisorRepository = supervisorRepository;
    this.academicStaffRepository = academicStaffRepository;
    this.organizationRepository = organizationRepository;
  }

  /**
   * Lists internships filtered server-side by the given criteria and scoped to the calling
   * principal. ADMIN sees all; SUPERVISOR sees internships assigned to them; FACULTY sees those
   * assigned to their academic staff profile; STUDENT sees their own.
   */
  public Page<InternshipResponse> list(
      PrincipalRole role,
      UUID appUserId,
      UUID studentId,
      UUID organizationId,
      UUID supervisorId,
      UUID academicStaffId,
      InternshipStatus status,
      String q,
      Pageable pageable) {
    UUID scopeStudentId = studentId;
    UUID scopeSupervisorId = supervisorId;
    UUID scopeAcademicStaffId = academicStaffId;
    if (role == PrincipalRole.STUDENT) {
      scopeStudentId = dataScope.studentId(appUserId);
      scopeSupervisorId = null;
      scopeAcademicStaffId = null;
      if (studentId != null && !studentId.equals(scopeStudentId)) {
        return Page.empty(pageable);
      }
    } else if (role == PrincipalRole.SUPERVISOR) {
      scopeSupervisorId = supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
      scopeAcademicStaffId = null;
    } else if (role == PrincipalRole.FACULTY) {
      scopeSupervisorId = null;
      scopeAcademicStaffId =
          academicStaffId != null ? academicStaffId : dataScope.academicStaffId(appUserId);
    }
    return internshipRepository
        .findAll(
            filters(
                scopeStudentId,
                organizationId,
                scopeSupervisorId,
                scopeAcademicStaffId,
                status,
                q),
            pageable)
        .map(InternshipResponse::from);
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Internship> filters(
      UUID studentId,
      UUID organizationId,
      UUID supervisorId,
      UUID academicStaffId,
      InternshipStatus status,
      String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (studentId != null) {
        predicates.add(cb.equal(root.get("studentId"), studentId));
      }
      if (organizationId != null) {
        predicates.add(cb.equal(root.get("organizationId"), organizationId));
      }
      if (supervisorId != null) {
        predicates.add(cb.equal(root.get("supervisorId"), supervisorId));
      }
      if (academicStaffId != null) {
        predicates.add(cb.equal(root.get("academicStaffId"), academicStaffId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (q != null && !q.isBlank()) {
        String like = "%" + q.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), like),
                cb.like(cb.lower(root.get("description")), like)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /** Fetches a single internship, verifying the principal may see it. Cross-scope get returns 404. */
  public InternshipResponse get(UUID id, PrincipalRole role, UUID appUserId) {
    Internship internship = find(id);
    if (role == PrincipalRole.STUDENT
        && !internship.getStudentId().equals(dataScope.studentId(appUserId))) {
      throw new ResourceNotFoundException(INTERNSHIP_NOT_FOUND, "Internship not found: " + id);
    }
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, internship.getStudentId())) {
      throw new ResourceNotFoundException(INTERNSHIP_NOT_FOUND, "Internship not found: " + id);
    }
    if (role == PrincipalRole.FACULTY
        && !dataScope.isAssignedToAcademicStudent(appUserId, internship.getStudentId())) {
      throw new ResourceNotFoundException(INTERNSHIP_NOT_FOUND, "Internship not found: " + id);
    }
    return InternshipResponse.from(internship);
  }

  /**
   * Returns the calling student's most recent internship assignment with the organization, company
   * supervisor and university supervisor names resolved. Empty when the student has no internship
   * yet. Missing profiles or references resolve to {@code null} names rather than failing.
   */
  public Optional<StudentInternshipResponse> myInternship(UUID appUserId) {
    UUID studentId = dataScope.studentId(appUserId);
    return internshipRepository
        .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
        .map(
            internship ->
                StudentInternshipResponse.from(
                    internship,
                    organizationRepository
                        .findById(internship.getOrganizationId())
                        .map(Organization::getName)
                        .orElse(null),
                    internship.getSupervisorId() == null
                        ? null
                        : supervisorRepository
                            .findById(internship.getSupervisorId())
                            .map(Supervisor::getFullName)
                            .orElse(null),
                    internship.getAcademicStaffId() == null
                        ? null
                        : academicStaffRepository
                            .findById(internship.getAcademicStaffId())
                            .map(AcademicStaff::getFullName)
                            .orElse(null)));
  }

  @Transactional
  public InternshipResponse create(InternshipRequest request) {
    validateSupervisorAssignments(
        request.studentId(),
        null,
        request.organizationId(),
        request.supervisorId(),
        request.academicStaffId(),
        true);
    Internship internship =
        internshipRepository.save(
            Internship.builder()
                .studentId(request.studentId())
                .organizationId(request.organizationId())
                .supervisorId(request.supervisorId())
                .academicStaffId(request.academicStaffId())
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(InternshipStatus.PENDING)
                .build());
    return InternshipResponse.from(internship);
  }

  @Transactional
  public InternshipResponse update(UUID id, InternshipRequest request) {
    Internship internship = find(id);
    validateSupervisorAssignments(
        internship.getStudentId(),
        internship.getId(),
        request.organizationId(),
        request.supervisorId(),
        request.academicStaffId(),
        isCurrent(internship));
    internship.update(
        request.organizationId(),
        request.supervisorId(),
        request.academicStaffId(),
        request.title(),
        request.description(),
        request.startDate(),
        request.endDate());
    return InternshipResponse.from(internshipRepository.save(internship));
  }

  @Transactional
  public void delete(UUID id) {
    internshipRepository.delete(find(id));
  }

  /** Assigns or removes the company and academic supervisors for an internship. */
  @Transactional
  public InternshipResponse assignSupervisors(
      UUID id, UUID supervisorId, UUID academicStaffId) {
    Internship internship = find(id);
    validateSupervisorAssignments(
        internship.getStudentId(),
        internship.getId(),
        internship.getOrganizationId(),
        supervisorId,
        academicStaffId,
        isCurrent(internship));
    internship.assignSupervisor(supervisorId);
    internship.assignAcademicStaff(academicStaffId);
    InternshipResponse response =
        InternshipResponse.from(internshipRepository.save(internship));
    auditLogService.record(
        AuditAction.INTERNSHIP_SUPERVISOR_ASSIGNED,
        "INTERNSHIP",
        internship.getId(),
        "Supervisor(s) assigned for internship "
            + internship.getTitle()
            + " (company: "
            + supervisorId
            + ", academic: "
            + academicStaffId
            + ").");
    return response;
  }

  /**
   * Role-scoped supervisor assignment. A supervisor may only reassign students currently under
   * their own supervision; every other role resolves to the plain assignment.
   */
  @Transactional
  public InternshipResponse assignSupervisors(
      UUID id, UUID supervisorId, UUID academicStaffId, PrincipalRole role, UUID appUserId) {
    if (role == PrincipalRole.SUPERVISOR) {
      Internship internship = find(id);
      if (!dataScope.isAssignedToStudent(appUserId, internship.getStudentId())) {
        throw new ResourceNotFoundException(INTERNSHIP_NOT_FOUND, "Internship not found: " + id);
      }
    }
    return assignSupervisors(id, supervisorId, academicStaffId);
  }

  /**
   * Transitions an internship into {@code target}. Only allowed from an active state (PENDING,
   * APPROVED) into a terminal or next state. Completing an internship requires a SUBMITTED
   * evaluation for the student+internship pair (SITMS-072 gate).
   */
  @Transactional
  public InternshipResponse changeStatus(UUID id, InternshipStatus target) {
    Internship internship = find(id);
    InternshipStatus current = internship.getStatus();
    if (!canTransition(current, target)) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION, "Cannot move " + current + " -> " + target);
    }
    if (target == InternshipStatus.COMPLETED
        && !evaluationRepository.existsByStudentIdAndInternshipIdAndStatus(
            internship.getStudentId(), internship.getId(), EvaluationStatus.SUBMITTED)) {
      throw new BusinessException(
          EVALUATION_REQUIRED_TO_COMPLETE,
          "A submitted evaluation is required before an internship can be completed.");
    }
    internship.changeStatus(target);
    InternshipResponse response =
        InternshipResponse.from(internshipRepository.save(internship));
    notifier.notify(
        studentAppUserId(internship.getStudentId()),
        NotificationType.INTERNSHIP_STATUS_CHANGE,
        "Internship status updated",
        "Your internship moved from " + current + " to " + target + ".",
        "/internships");
    auditLogService.record(
        AuditAction.INTERNSHIP_STATUS_CHANGE,
        "INTERNSHIP",
        internship.getId(),
        "Internship status changed from " + current + " to " + target + ".");
    return response;
  }

  private static boolean canTransition(InternshipStatus current, InternshipStatus target) {
    if (current == target) {
      return true;
    }
    return switch (current) {
      case PENDING -> target == InternshipStatus.APPROVED || target == InternshipStatus.REJECTED;
      case APPROVED -> target == InternshipStatus.COMPLETED || target == InternshipStatus.CANCELLED;
      default -> false;
    };
  }

  private Internship find(UUID id) {
    return internshipRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    INTERNSHIP_NOT_FOUND, "Internship not found: " + id));
  }

/**
   * Validates supervisor references before save/assign:
   *
   * <ul>
   *   <li>the company supervisor must belong to the internship organization and be active;
   *   <li>the university supervisor must be active;
   *   <li>a student may carry at most one company supervisor and at most one university supervisor
   *       across their current internships (PENDING or APPROVED) — one student must never be
   *       double-assigned to two supervisors at the same time. Terminal internships (REJECTED,
   *       COMPLETED, CANCELLED) neither block nor count towards the rule, so editing only their
   *       (historical) supervisor references is always allowed.
   * </ul>
   */
  private void validateSupervisorAssignments(
      UUID studentId,
      UUID exceptInternshipId,
      UUID organizationId,
      UUID supervisorId,
      UUID academicStaffId,
      boolean targetActive) {
    if (supervisorId != null) {
      Supervisor supervisor =
          supervisorRepository
              .findById(supervisorId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "SUPERVISOR_NOT_FOUND", "Supervisor not found: " + supervisorId));
      if (!supervisor.getOrganizationId().equals(organizationId)) {
        throw new BusinessException(
            SUPERVISOR_ORGANIZATION_MISMATCH,
            "Company supervisor must belong to the internship organization.");
      }
      if (!supervisor.isActive()) {
        throw new BusinessException(
            SUPERVISOR_INACTIVE, "An inactive company supervisor cannot be assigned.");
      }
      ensureSingleSupervisorPerStudent(
          studentId,
          exceptInternshipId,
          supervisorId,
          Internship::getSupervisorId,
          STUDENT_ALREADY_ASSIGNED_COMPANY_SUPERVISOR,
          "company supervisor",
          existing ->
              supervisorRepository
                  .findById(existing)
                  .map(Supervisor::getFullName)
                  .orElse(existing.toString()),
          targetActive);
    }
    if (academicStaffId != null) {
      AcademicStaff academicStaff =
          academicStaffRepository
              .findById(academicStaffId)
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "ACADEMIC_STAFF_NOT_FOUND",
                          "Academic supervisor not found: " + academicStaffId));
      if (!academicStaff.isActive()) {
        throw new BusinessException(
            ACADEMIC_STAFF_INACTIVE,
            "An inactive university supervisor cannot be assigned.");
      }
      ensureSingleSupervisorPerStudent(
          studentId,
          exceptInternshipId,
          academicStaffId,
          Internship::getAcademicStaffId,
          STUDENT_ALREADY_ASSIGNED_UNIVERSITY_SUPERVISOR,
          "university supervisor",
          existing ->
              academicStaffRepository
                  .findById(existing)
                  .map(AcademicStaff::getFullName)
                  .orElse(existing.toString()),
          targetActive);
    }
  }

  private boolean isCurrent(Internship internship) {
    return internship.getStatus() == InternshipStatus.PENDING
        || internship.getStatus() == InternshipStatus.APPROVED;
  }

  /**
   * Ensures the student never ends up with two distinct current supervisors of the same kind.
   * Editing a terminal internship never counts towards the rule.
   */
  private void ensureSingleSupervisorPerStudent(
      UUID studentId,
      UUID exceptInternshipId,
      UUID incomingSupervisorId,
      java.util.function.Function<Internship, UUID> supervisorOf,
      String errorCode,
      String kind,
      java.util.function.Function<UUID, String> nameResolver,
      boolean targetActive) {
    if (!targetActive) {
      return;
    }
    internshipRepository
        .findAllByStudentId(studentId)
        .stream()
        .filter(other -> !other.getId().equals(exceptInternshipId))
        .filter(other -> isCurrent(other))
        .map(supervisorOf)
        .filter(Objects::nonNull)
        .distinct()
        .filter(assigned -> !assigned.equals(incomingSupervisorId))
        .findFirst()
        .ifPresent(
            existing -> {
              throw new BusinessException(
                  errorCode,
                  "This student is already assigned to "
                      + kind
                      + " "
                      + nameResolver.apply(existing)
                      + ".");
            });
  }

  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }
}
