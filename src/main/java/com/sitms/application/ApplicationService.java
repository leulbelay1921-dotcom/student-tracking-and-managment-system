package com.sitms.application;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.internship.InternshipStatus;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.settings.AppSettingService;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for {@link Application} (SITMS M2).
 *
 * <p>Students apply to an internship (one active track at a time); admins decide any application,
 * supervisors only applications of students assigned to them; approval materializes the placement
 * (an {@code Internship} row, born {@code APPROVED}).
 */
@Service
public class ApplicationService {

  public static final String APPLICATION_NOT_FOUND = "APPLICATION_NOT_FOUND";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String REGISTRATION_CLOSED = "REGISTRATION_CLOSED";
  public static final String APPLICATION_ALREADY_EXISTS = "APPLICATION_ALREADY_EXISTS";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

  private static final List<ApplicationStatus> ACTIVE_APPLICATION_STATUSES =
      List.of(ApplicationStatus.PENDING, ApplicationStatus.APPROVED);
  private static final List<InternshipStatus> ACTIVE_PLACEMENT_STATUSES =
      List.of(InternshipStatus.APPROVED, InternshipStatus.COMPLETED);

  private final ApplicationRepository applicationRepository;
  private final InternshipRepository internshipRepository;
  private final StudentRepository studentRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;
  private final AppSettingService appSettingService;
  private final DataScope dataScope;

  public ApplicationService(
      ApplicationRepository applicationRepository,
      InternshipRepository internshipRepository,
      StudentRepository studentRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService,
      AppSettingService appSettingService,
      DataScope dataScope) {
    this.applicationRepository = applicationRepository;
    this.internshipRepository = internshipRepository;
    this.studentRepository = studentRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
    this.appSettingService = appSettingService;
    this.dataScope = dataScope;
  }

  /**
   * Lists applications filtered server-side and scoped to the calling principal. ADMIN sees all;
   * SUPERVISOR sees applications of their assigned students; STUDENT sees only their own.
   * Cross-scope requests are silently empty.
   */
  @Transactional(readOnly = true)
  public Page<ApplicationResponse> list(
      PrincipalRole role,
      UUID appUserId,
      UUID studentId,
      UUID organizationId,
      UUID supervisorId,
      UUID academicStaffId,
      ApplicationStatus status,
      String q,
      Pageable pageable) {
    UUID scopeStudentId = studentId;
    Set<UUID> scopeStudents = null;
    if (role == PrincipalRole.STUDENT) {
      UUID own = dataScope.studentId(appUserId);
      if (studentId != null && !studentId.equals(own)) {
        return Page.empty(pageable);
      }
      scopeStudentId = own;
    } else if (role == PrincipalRole.SUPERVISOR) {
      UUID targetSupervisorId =
          supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
      scopeStudents = internshipRepository.findStudentIdsBySupervisorId(targetSupervisorId);
      scopeStudentId = null;
    } else if (role == PrincipalRole.FACULTY) {
      scopeStudents =
          academicStaffId != null
              ? internshipRepository.findStudentIdsByAcademicStaffId(academicStaffId)
              : internshipRepository.findStudentIdsByAcademicStaffId(
                  dataScope.academicStaffId(appUserId));
      scopeStudentId = null;
    }
    return applicationRepository
        .findAll(filters(scopeStudentId, organizationId, scopeStudents, status, q), pageable)
        .map(ApplicationResponse::from);
  }

  /** Fetches a single application, verifying the principal may see it. Cross-scope get returns 404. */
  @Transactional(readOnly = true)
  public ApplicationResponse get(UUID id, PrincipalRole role, UUID appUserId) {
    Application application = find(id);
    if (role == PrincipalRole.STUDENT
        && !application.getStudentId().equals(dataScope.studentId(appUserId))) {
      throw new ResourceNotFoundException(APPLICATION_NOT_FOUND, "Application not found: " + id);
    }
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, application.getStudentId())) {
      throw new ResourceNotFoundException(APPLICATION_NOT_FOUND, "Application not found: " + id);
    }
    return ApplicationResponse.from(application);
  }

  /** Submits a new application on behalf of the calling student (self-service, single-track). */
  @Transactional
  public ApplicationResponse apply(UUID appUserId, ApplicationRequest request) {
    if (!appSettingService.isEnabled("registration_open", true)) {
      throw new BusinessException(REGISTRATION_CLOSED, "Internship registration is currently closed.");
    }
    UUID studentId =
        studentRepository
            .findByAppUserId(appUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        STUDENT_PROFILE_NOT_FOUND, "No student profile for user: " + appUserId))
            .getId();
    if (!canApply(studentId)) {
      throw new BusinessException(
          APPLICATION_ALREADY_EXISTS,
          "A student may only hold one active application or placement at a time.");
    }
    Application application =
        applicationRepository.saveAndFlush(
            Application.builder()
                .studentId(studentId)
                .organizationId(request.organizationId())
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build());
    auditLogService.record(
        AuditAction.APPLICATION_SUBMITTED,
        "APPLICATION",
        application.getId(),
        "Internship application submitted.");
    return ApplicationResponse.from(application);
  }

  /** Decides a pending application. Approving materializes the internship placement. */
  @Transactional
  public ApplicationResponse decide(
      UUID id, ApplicationStatus target, PrincipalRole role, UUID appUserId) {
    if (target != ApplicationStatus.APPROVED && target != ApplicationStatus.REJECTED) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION, "Applications can only be approved or rejected: " + target);
    }
    Application application = find(id);
    if (application.getStatus() != ApplicationStatus.PENDING) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only pending applications can be decided, current status: " + application.getStatus());
    }
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, application.getStudentId())) {
      throw new AccessDeniedException(
          "Student is not assigned to this supervisor: " + application.getStudentId());
    }
    application.changeStatus(target);
    if (target == ApplicationStatus.APPROVED) {
      createPlacement(application);
    }
    ApplicationResponse response =
        ApplicationResponse.from(applicationRepository.save(application));
    notifier.notify(
        studentAppUserId(application.getStudentId()),
        NotificationType.APPLICATION_STATUS_CHANGE,
        "Application " + target.name().toLowerCase(),
        "Your internship application \"" + application.getTitle()
            + "\" was " + target.name().toLowerCase() + ".",
        "/applications");
    auditLogService.record(
        AuditAction.APPLICATION_DECIDED,
        "APPLICATION",
        application.getId(),
        "Internship application " + target.name().toLowerCase() + ".");
    return response;
  }

  /** Withdraws a pending application; only the owning student may withdraw their own. */
  @Transactional
  public ApplicationResponse withdraw(UUID id, UUID appUserId) {
    UUID studentId = dataScope.studentId(appUserId);
    Application application = find(id);
    if (!application.getStudentId().equals(studentId)) {
      throw new ResourceNotFoundException(APPLICATION_NOT_FOUND, "Application not found: " + id);
    }
    if (application.getStatus() != ApplicationStatus.PENDING) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only pending applications can be withdrawn, current status: " + application.getStatus());
    }
    application.changeStatus(ApplicationStatus.WITHDRAWN);
    ApplicationResponse response =
        ApplicationResponse.from(applicationRepository.save(application));
    auditLogService.record(
        AuditAction.APPLICATION_DECIDED,
        "APPLICATION",
        application.getId(),
        "Internship application withdrawn.");
    return response;
  }

  private boolean canApply(UUID studentId) {
    if (applicationRepository.existsByStudentIdAndStatusIn(studentId, ACTIVE_APPLICATION_STATUSES)) {
      return false;
    }
    return !internshipRepository.existsByStudentIdAndStatusIn(studentId, ACTIVE_PLACEMENT_STATUSES);
  }

  private void createPlacement(Application application) {
    internshipRepository.save(
        Internship.builder()
            .studentId(application.getStudentId())
            .organizationId(application.getOrganizationId())
            .title(application.getTitle())
            .description(application.getDescription())
            .startDate(application.getStartDate())
            .endDate(application.getEndDate())
            .status(InternshipStatus.APPROVED)
            .build());
  }

  private Application find(UUID id) {
    return applicationRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    APPLICATION_NOT_FOUND, "Application not found: " + id));
  }

  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Application> filters(
      UUID studentId, UUID organizationId, Set<UUID> students, ApplicationStatus status, String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (studentId != null) {
        predicates.add(cb.equal(root.get("studentId"), studentId));
      }
      if (organizationId != null) {
        predicates.add(cb.equal(root.get("organizationId"), organizationId));
      }
      if (students != null) {
        predicates.add(root.get("studentId").in(students));
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
}