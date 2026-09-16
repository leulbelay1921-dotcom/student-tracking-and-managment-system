package com.sitms.activity;

import com.sitms.academicstaff.AcademicStaff;
import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.attachment.AttachmentInfo;
import com.sitms.attachment.AttachmentOwnerType;
import com.sitms.attachment.AttachmentService;
import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.Supervisor;
import com.sitms.supervisor.SupervisorRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link Activity} and its review workflow (SITMS-028). */
@Service
public class ActivityService {

  public static final String ACTIVITY_NOT_FOUND = "ACTIVITY_NOT_FOUND";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String SUPERVISOR_PROFILE_NOT_FOUND = "SUPERVISOR_PROFILE_NOT_FOUND";
  public static final String ACADEMIC_STAFF_PROFILE_NOT_FOUND = "ACADEMIC_STAFF_PROFILE_NOT_FOUND";
  public static final String REJECT_REASON_REQUIRED = "REJECT_REASON_REQUIRED";

  private final ActivityRepository activityRepository;
  private final ActivityReviewRepository reviewRepository;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final AcademicStaffRepository academicStaffRepository;
  private final InternshipRepository internshipRepository;
  private final AttachmentService attachmentService;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;
  private final DataScope dataScope;

  public ActivityService(
      ActivityRepository activityRepository,
      ActivityReviewRepository reviewRepository,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      AcademicStaffRepository academicStaffRepository,
      InternshipRepository internshipRepository,
      AttachmentService attachmentService,
      NotificationNotifier notifier,
      AuditLogService auditLogService,
      DataScope dataScope) {
    this.activityRepository = activityRepository;
    this.reviewRepository = reviewRepository;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.academicStaffRepository = academicStaffRepository;
    this.internshipRepository = internshipRepository;
    this.attachmentService = attachmentService;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
    this.dataScope = dataScope;
  }

  /**
   * Lists activities filtered server-side by the given criteria and scoped to the calling
   * principal. ADMIN sees all; SUPERVISOR sees activities of their assigned students; STUDENT sees
   * their own.
   */
  public Page<ActivityResponse> list(
      PrincipalRole role,
      UUID appUserId,
      UUID studentId,
      UUID internshipId,
      UUID supervisorId,
      UUID academicStaffId,
      ActivityStatus status,
      LocalDate from,
      LocalDate to,
      String q,
      Pageable pageable) {
    UUID scopeStudentId = studentId;
    UUID scopeSupervisorId = supervisorId;
    UUID scopeAcademicStaffId = null;
    if (role == PrincipalRole.STUDENT) {
      UUID own = dataScope.studentId(appUserId);
      scopeStudentId = own;
      scopeSupervisorId = null;
      if (studentId != null && !studentId.equals(own)) {
        return Page.empty(pageable);
      }
    } else if (role == PrincipalRole.SUPERVISOR) {
      scopeSupervisorId = supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
    } else if (role == PrincipalRole.FACULTY) {
      scopeSupervisorId = null;
      scopeAcademicStaffId =
          academicStaffId != null ? academicStaffId : dataScope.academicStaffId(appUserId);
    }
    return mapWithReviews(
        activityRepository.findAll(
            filters(
                scopeStudentId, internshipId, scopeSupervisorId, scopeAcademicStaffId, status, from, to, q),
            pageable));
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Activity> filters(
      UUID studentId,
      UUID internshipId,
      UUID supervisorId,
      UUID academicStaffId,
      ActivityStatus status,
      LocalDate from,
      LocalDate to,
      String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (studentId != null) {
        predicates.add(cb.equal(root.get("studentId"), studentId));
      }
      if (internshipId != null) {
        predicates.add(cb.equal(root.get("internshipId"), internshipId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("activityDate"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("activityDate"), to));
      }
      if (supervisorId != null) {
        jakarta.persistence.criteria.Subquery<UUID> sub =
            query.subquery(UUID.class);
        var internshipRoot = sub.from(com.sitms.internship.Internship.class);
        sub.select(internshipRoot.get("studentId"))
            .where(cb.equal(internshipRoot.get("supervisorId"), supervisorId));
        predicates.add(root.get("studentId").in(sub));
      }
      if (academicStaffId != null) {
        jakarta.persistence.criteria.Subquery<UUID> sub = query.subquery(UUID.class);
        var internshipRoot = sub.from(com.sitms.internship.Internship.class);
        sub.select(internshipRoot.get("studentId"))
            .where(cb.equal(internshipRoot.get("academicStaffId"), academicStaffId));
        predicates.add(root.get("studentId").in(sub));
      }
      if (q != null && !q.isBlank()) {
        String like = "%" + q.trim().toLowerCase() + "%";
        predicates.add(cb.like(cb.lower(root.get("description")), like));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /** Fetches a single activity, verifying the principal may see it. Cross-scope get returns 404. */
  public ActivityResponse get(UUID id, PrincipalRole role, UUID appUserId) {
    Activity activity = find(id);
    if (role == PrincipalRole.STUDENT
        && !activity.getStudentId().equals(dataScope.studentId(appUserId))) {
      throw new ResourceNotFoundException(ACTIVITY_NOT_FOUND, "Activity not found: " + id);
    }
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, activity.getStudentId())) {
      throw new ResourceNotFoundException(ACTIVITY_NOT_FOUND, "Activity not found: " + id);
    }
    if (role == PrincipalRole.FACULTY
        && !dataScope.isAssignedToAcademicStudent(appUserId, activity.getStudentId())) {
      throw new ResourceNotFoundException(ACTIVITY_NOT_FOUND, "Activity not found: " + id);
    }
    return ActivityResponse.from(
        activity,
        findReview(activity.getId()),
        attachmentService.list(AttachmentOwnerType.ACTIVITY, activity.getId()));
  }

  /**
   * Activities awaiting (or after) review. ADMIN sees all; supervisors see only their assigned
   * students at the company stage; academic staff (FACULTY) see only their assigned students at
   * the university stage (SITMS-031). Optional {@code supervisorId}/{@code academicStaffId} overrides
   * the scope (same role only), and {@code studentId} narrows to a single student.
   */
  public Page<ActivityResponse> reviewQueue(
      PrincipalRole role,
      UUID appUserId,
      ActivityStatus status,
      UUID supervisorId,
      UUID academicStaffId,
      UUID studentId,
      Pageable pageable) {
    if (studentId != null) {
      return mapWithReviews(activityRepository.findByStatusAndStudentId(status, studentId, pageable));
    }
    if (role == PrincipalRole.ADMIN) {
      if (supervisorId != null) {
        return mapWithReviews(
            activityRepository.findByStatusAndAssignedSupervisor(status, supervisorId, pageable));
      }
      if (academicStaffId != null) {
        return mapWithReviews(
            activityRepository.findByStatusAndAssignedAcademicStaff(status, academicStaffId, pageable));
      }
      return mapWithReviews(activityRepository.findByStatus(status, pageable));
    }
    if (role == PrincipalRole.FACULTY) {
      UUID staffId = academicStaffId != null ? academicStaffId : resolveAcademicStaffId(appUserId);
      return mapWithReviews(
          activityRepository.findByStatusAndAssignedAcademicStaff(status, staffId, pageable));
    }
    UUID resolvedSupervisorId =
        supervisorId != null ? supervisorId : resolveSupervisorId(appUserId);
    return mapWithReviews(
        activityRepository.findByStatusAndAssignedSupervisor(
            status, resolvedSupervisorId, pageable));
  }

  /**
   * Log an activity. Students (self-service) are forced to their own profile; admins log on behalf
   * of a student via the request's studentId.
   */
  @Transactional
  public ActivityResponse create(ActivityRequest request, UUID appUserId, boolean selfService) {
    UUID studentId =
        selfService
            ? studentRepository
                .findByAppUserId(appUserId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            STUDENT_PROFILE_NOT_FOUND,
                            "No student profile for user: " + appUserId))
                .getId()
            : requireStudentId(request.studentId());
    Activity activity =
        activityRepository.saveAndFlush(
            Activity.builder()
                .studentId(studentId)
                .internshipId(request.internshipId())
                .activityDate(request.activityDate())
                .type(request.type())
                .title(request.title())
                .description(request.description())
                .skills(request.skills())
                .hours(request.hours())
                .status(ActivityStatus.DRAFT)
                .build());
    attachmentService.replace(
        AttachmentOwnerType.ACTIVITY, activity.getId(), request.attachmentDocumentIds(), studentId);
    return ActivityResponse.from(
        activity,
        null,
        attachmentService.list(AttachmentOwnerType.ACTIVITY, activity.getId()));
  }

  @Transactional
  public ActivityResponse update(UUID id, ActivityRequest request) {
    return update(id, request, null, false);
  }

  /**
   * Updates an activity. Admins edit anything; a student (self-service) may only revise their own
   * activity while it is a DRAFT or was returned for revision.
   */
  @Transactional
  public ActivityResponse update(
      UUID id, ActivityRequest request, UUID appUserId, boolean selfService) {
    Activity activity = find(id);
    if (selfService) {
      if (!activity.getStudentId().equals(dataScope.studentId(appUserId))) {
        throw new BusinessException(
            ACTIVITY_NOT_FOUND, "Activity not found for this student: " + id);
      }
      if (activity.getStatus() != ActivityStatus.DRAFT
          && activity.getStatus() != ActivityStatus.REVISION_REQUESTED) {
        throw new BusinessException(
            INVALID_STATUS_TRANSITION,
            "Only draft or revision-requested activities can be edited: " + activity.getStatus());
      }
    }
    activity.update(
        request.internshipId(),
        request.activityDate(),
        request.type(),
        request.title(),
        request.description(),
        request.skills(),
        request.hours());
    activityRepository.saveAndFlush(activity);
    attachmentService.replace(
        AttachmentOwnerType.ACTIVITY, activity.getId(), request.attachmentDocumentIds(), activity.getStudentId());
    return ActivityResponse.from(
        activity,
        findReview(activity.getId()),
        attachmentService.list(AttachmentOwnerType.ACTIVITY, activity.getId()));
  }

  @Transactional
  public void delete(UUID id) {
    Activity activity = find(id);
    attachmentService.deleteForOwner(AttachmentOwnerType.ACTIVITY, id);
    reviewRepository.findByActivityId(id).ifPresent(reviewRepository::delete);
    activityRepository.delete(activity);
  }

  /** Student submits one of their own DRAFT activities for review. */
  @Transactional
  public ActivityResponse submit(UUID id, UUID appUserId) {
    UUID studentId =
        studentRepository
            .findByAppUserId(appUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        STUDENT_PROFILE_NOT_FOUND,
                        "No student profile for user: " + appUserId))
            .getId();
    Activity activity = find(id);
    if (!activity.getStudentId().equals(studentId)) {
      throw new BusinessException(ACTIVITY_NOT_FOUND, "Activity not found for this student: " + id);
    }
    transition(activity, ActivityStatus.SUBMITTED);
    ActivityResponse response =
        ActivityResponse.from(
            activityRepository.saveAndFlush(activity),
            null,
            attachmentService.list(AttachmentOwnerType.ACTIVITY, activity.getId()));
    notifier.notify(
        supervisorAppUserId(activity.getInternshipId()),
        NotificationType.ACTIVITY_SUBMITTED,
        "New activity awaiting review",
        "A student submitted an activity log for your review.",
        null);
    notifier.notify(
        academicStaffAppUserId(activity.getInternshipId()),
        NotificationType.ACTIVITY_SUBMITTED,
        "New activity submitted",
        "A student submitted an activity log. It is visible to you as the university supervisor.",
        null);
    notifier.notifyAdmins(
        NotificationType.ACTIVITY_SUBMITTED,
        "New activity submitted",
        "A student submitted an activity log for review.",
        null);
    auditLogService.record(
        AuditAction.ACTIVITY_SUBMITTED,
        "ACTIVITY",
        activity.getId(),
        "Activity submitted for review.");
    return response;
  }

  /**
   * Reviews an activity. The company supervisor (or an admin on their behalf) decides on SUBMITTED
   * activities; the university supervisor (or an admin) decides on COMPANY_APPROVED activities.
   * Rejecting or requesting a revision always requires a reason.
   */
  @Transactional
  public ActivityResponse review(
      UUID id, PrincipalRole role, UUID appUserId, ActivityReviewRequest request) {
    Activity activity = find(id);
    if (activity.getStatus() == ActivityStatus.SUBMITTED) {
      return companyReview(activity, role, appUserId, request);
    }
    if (activity.getStatus() == ActivityStatus.COMPANY_APPROVED) {
      return academicReview(activity, role, appUserId, request);
    }
    throw new BusinessException(
        INVALID_STATUS_TRANSITION,
        "Only submitted or company-approved activities can be reviewed: "
            + activity.getStatus());
  }

  /** Stage one: the company supervisor approves or rejects a submitted activity. */
  private ActivityResponse companyReview(
      Activity activity, PrincipalRole role, UUID appUserId, ActivityReviewRequest request) {
    if (role == PrincipalRole.FACULTY) {
      throw new AccessDeniedException(
          "Only company supervisors or admins decide the company stage: " + activity.getId());
    }
    UUID reviewerId =
        role == PrincipalRole.ADMIN
            ? internshipSupervisorId(activity.getInternshipId())
            : resolveSupervisorId(appUserId);
    if (role != PrincipalRole.ADMIN
        && !internshipRepository.existsBySupervisorIdAndStudentId(
            reviewerId, activity.getStudentId())) {
      throw new AccessDeniedException(
          "Activity is not assigned to this supervisor: " + activity.getId());
    }
    if (request.decision() == ReviewDecision.REJECTED
        && (request.feedback() == null || request.feedback().isBlank())) {
      throw new BusinessException(
          REJECT_REASON_REQUIRED, "A rejection reason is required.");
    }

    ActivityReview review =
        reviewRepository
            .findByActivityId(activity.getId())
            .orElseGet(() -> newReview(activity.getId(), reviewerId));
    review.update(request.decision(), request.feedback());
    reviewRepository.saveAndFlush(review);

    boolean approved = request.decision() == ReviewDecision.APPROVED;
    transition(
        activity,
        approved ? ActivityStatus.COMPANY_APPROVED : ActivityStatus.COMPANY_REJECTED);
    ActivityResponse response =
        ActivityResponse.from(
            activityRepository.saveAndFlush(activity),
            review,
            attachmentService.list(AttachmentOwnerType.ACTIVITY, activity.getId()));
    notifier.notify(
        studentAppUserId(activity.getStudentId()),
        NotificationType.ACTIVITY_REVIEWED,
        approved ? "Activity approved by the company" : "Activity rejected by the company",
        request.feedback() == null ? null : "Feedback: " + request.feedback(),
        "/activities");
    auditLogService.record(
        AuditAction.ACTIVITY_REVIEWED,
        "ACTIVITY",
        activity.getId(),
        "Company " + (approved ? "approved" : "rejected") + " the activity.");
    return response;
  }

  /** Stage two: the university supervisor approves the activity or requests a revision. */
  private ActivityResponse academicReview(
      Activity activity, PrincipalRole role, UUID appUserId, ActivityReviewRequest request) {
    if (role == PrincipalRole.SUPERVISOR) {
      throw new AccessDeniedException(
          "Only university supervisors or admins decide the academic stage: "
              + activity.getId());
    }
    UUID reviewerId =
        role == PrincipalRole.ADMIN
            ? internshipAcademicStaffId(activity.getInternshipId())
            : resolveAcademicStaffId(appUserId);
    if (role != PrincipalRole.ADMIN
        && !dataScope.isAssignedToAcademicStudent(appUserId, activity.getStudentId())) {
      throw new AccessDeniedException(
          "Student is not assigned to this academic staff member: " + activity.getStudentId());
    }
    if (request.decision() == ReviewDecision.REJECTED
        && (request.feedback() == null || request.feedback().isBlank())) {
      throw new BusinessException(
          REJECT_REASON_REQUIRED, "A revision reason is required.");
    }

    boolean approved = request.decision() == ReviewDecision.APPROVED;
    activity.applyAcademicReview(reviewerId, request.feedback());
    transition(
        activity,
        approved ? ActivityStatus.APPROVED : ActivityStatus.REVISION_REQUESTED);
    ActivityResponse response =
        ActivityResponse.from(
            activityRepository.saveAndFlush(activity),
            findReview(activity.getId()),
            attachmentService.list(AttachmentOwnerType.ACTIVITY, activity.getId()));
    notifier.notify(
        studentAppUserId(activity.getStudentId()),
        NotificationType.ACTIVITY_REVIEWED,
        approved ? "Activity fully approved" : "Activity revision requested",
        request.feedback() == null ? null : "Feedback: " + request.feedback(),
        "/activities");
    auditLogService.record(
        AuditAction.ACTIVITY_REVIEWED,
        "ACTIVITY",
        activity.getId(),
        approved
            ? "University supervisor approved the activity."
            : "University supervisor requested a revision.");
    return response;
  }

  private ActivityReview newReview(UUID activityId, UUID reviewerId) {
    ActivityReview review =
        ActivityReview.builder()
            .activityId(activityId)
            .reviewerId(reviewerId)
            .decision(ReviewDecision.APPROVED)
            .build();
    return reviewRepository.saveAndFlush(review);
  }

  private static void transition(Activity activity, ActivityStatus target) {
    ActivityStatus current = activity.getStatus();
    if (!canTransition(current, target)) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION, "Cannot move " + current + " -> " + target);
    }
    activity.changeStatus(target);
  }

  private static boolean canTransition(ActivityStatus current, ActivityStatus target) {
    if (current == target) {
      return true;
    }
    return switch (current) {
      case DRAFT -> target == ActivityStatus.SUBMITTED;
      case SUBMITTED ->
          target == ActivityStatus.COMPANY_APPROVED || target == ActivityStatus.COMPANY_REJECTED;
      case COMPANY_APPROVED ->
          target == ActivityStatus.APPROVED || target == ActivityStatus.REVISION_REQUESTED;
      case REVISION_REQUESTED -> target == ActivityStatus.SUBMITTED;
      default -> false;
    };
  }

  private Page<ActivityResponse> mapWithReviews(Page<Activity> page) {
    Map<UUID, ActivityReview> reviewsById =
        reviewRepository
            .findByActivityIdIn(page.getContent().stream().map(Activity::getId).toList())
            .stream()
            .collect(Collectors.toMap(ActivityReview::getActivityId, r -> r));
    Map<UUID, List<AttachmentInfo>> attachmentsById =
        attachmentService.listByOwners(
            AttachmentOwnerType.ACTIVITY, page.getContent().stream().map(Activity::getId).toList());
    return page.map(
        a -> ActivityResponse.from(a, reviewsById.get(a.getId()), attachmentsById.getOrDefault(a.getId(), List.of())));
  }

  private ActivityReview findReview(UUID activityId) {
    return reviewRepository.findByActivityId(activityId).orElse(null);
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

  /** The supervisor of the internship that owns the given activity. */
  private UUID internshipSupervisorId(UUID internshipId) {
    return internshipRepository
        .findById(internshipId)
        .map(Internship::getSupervisorId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    SUPERVISOR_PROFILE_NOT_FOUND,
                    "No supervisor for activity internship: " + internshipId));
  }

  /** The academic staff member assigned to the internship that owns the given activity. */
  private UUID internshipAcademicStaffId(UUID internshipId) {
    return internshipRepository
        .findById(internshipId)
        .map(Internship::getAcademicStaffId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ACADEMIC_STAFF_PROFILE_NOT_FOUND,
                    "No academic staff for activity internship: " + internshipId));
  }

  /** The app_user id of the supervisor of the internship that owns the given activity, or null. */
  private UUID supervisorAppUserId(UUID internshipId) {
    if (internshipId == null) {
      return null;
    }
    return internshipRepository
        .findById(internshipId)
        .map(Internship::getSupervisorId)
        .filter(java.util.Objects::nonNull)
        .flatMap(supervisorRepository::findById)
        .map(Supervisor::getAppUserId)
        .orElse(null);
  }

  /** The app_user id of the academic staff (university supervisor) on the internship, or null. */
  private UUID academicStaffAppUserId(UUID internshipId) {
    if (internshipId == null) {
      return null;
    }
    return internshipRepository
        .findById(internshipId)
        .map(Internship::getAcademicStaffId)
        .filter(java.util.Objects::nonNull)
        .flatMap(academicStaffRepository::findById)
        .map(AcademicStaff::getAppUserId)
        .orElse(null);
  }

  /** The app_user id of the student profile, or null. */
  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }

  /** Non-self-service creates must name a student; self-service resolves it via appUserId. */
  private UUID requireStudentId(UUID studentId) {
    if (studentId == null) {
      throw new BusinessException(STUDENT_PROFILE_NOT_FOUND, "studentId is required");
    }
    return studentId;
  }

  private Activity find(UUID id) {
    return activityRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException(ACTIVITY_NOT_FOUND, "Activity not found: " + id));
  }
}
