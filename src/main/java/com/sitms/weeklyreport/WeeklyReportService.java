package com.sitms.weeklyreport;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for weekly reports and their two-stage approval: the company supervisor reviews
 * first, then the academic staff decides (SITMS-077). Students may edit a report until it is fully
 * approved; editing re-queues it for the company stage.
 */
@Service
public class WeeklyReportService {

  public static final String WEEKLY_REPORT_NOT_FOUND = "WEEKLY_REPORT_NOT_FOUND";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  public static final String WEEK_NUMBER_REQUIRED = "WEEK_NUMBER_REQUIRED";

  private final WeeklyReportRepository weeklyReportRepository;
  private final AttachmentService attachmentService;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final InternshipRepository internshipRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;
  private final DataScope dataScope;

  public WeeklyReportService(
      WeeklyReportRepository weeklyReportRepository,
      AttachmentService attachmentService,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      InternshipRepository internshipRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService,
      DataScope dataScope) {
    this.weeklyReportRepository = weeklyReportRepository;
    this.attachmentService = attachmentService;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.internshipRepository = internshipRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
    this.dataScope = dataScope;
  }

  /** A student creates and submits a weekly report. */
  @Transactional
  public WeeklyReportResponse create(UUID appUserId, WeeklyReportRequest request) {
    UUID studentId = dataScope.studentId(appUserId);
    WeeklyReport saved =
        weeklyReportRepository.saveAndFlush(
            WeeklyReport.builder()
                .studentId(studentId)
                .internshipId(request.internshipId())
                .weekNumber(request.weekNumber())
                .title(request.title())
                .content(request.content() == null ? "" : request.content())
                .activitiesCompleted(request.activitiesCompleted())
                .problemsEncountered(request.problemsEncountered())
                .skillsLearned(request.skillsLearned())
                .nextWeekPlan(request.nextWeekPlan())
                .status(WeeklyReportStatus.PENDING_COMPANY)
                .build());
    attachmentService.replace(
        AttachmentOwnerType.WEEKLY_REPORT,
        saved.getId(),
        request.attachmentDocumentIds() == null ? List.of() : request.attachmentDocumentIds(),
        studentId);
    notifier.notify(
        companySupervisorAppUserId(saved.getInternshipId()),
        NotificationType.WEEKLY_REPORT_SUBMITTED,
        "Weekly report awaiting review",
        "Week " + saved.getWeekNumber() + " report submitted for your approval.",
        "/weekly-reports");
    auditLogService.record(
        AuditAction.WEEKLY_REPORT_SUBMITTED,
        "WEEKLY_REPORT",
        saved.getId(),
        "Weekly report for week " + saved.getWeekNumber() + " submitted.");
    return WeeklyReportResponse.from(
        saved, studentName(studentId), attachments(saved.getId()));
  }

  /** A student updates their own report while it is not yet fully approved. */
  @Transactional
  public WeeklyReportResponse update(
      UUID appUserId, UUID id, WeeklyReportRequest request) {
    UUID studentId = dataScope.studentId(appUserId);
    WeeklyReport report = find(id);
    if (!report.getStudentId().equals(studentId)) {
      throw new AccessDeniedException("Weekly report does not belong to this student: " + id);
    }
    if (report.getStatus() == WeeklyReportStatus.APPROVED) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION, "An approved weekly report can no longer be edited.");
    }
    report.update(
        request.title(),
        request.content() == null ? "" : request.content(),
        request.weekNumber(),
        request.internshipId(),
        request.activitiesCompleted(),
        request.problemsEncountered(),
        request.skillsLearned(),
        request.nextWeekPlan());
    WeeklyReport saved = weeklyReportRepository.saveAndFlush(report);
    attachmentService.replace(
        AttachmentOwnerType.WEEKLY_REPORT,
        saved.getId(),
        request.attachmentDocumentIds() == null ? List.of() : request.attachmentDocumentIds(),
        studentId);
    notifier.notify(
        companySupervisorAppUserId(saved.getInternshipId()),
        NotificationType.WEEKLY_REPORT_SUBMITTED,
        "Weekly report re-submitted",
        "Week " + saved.getWeekNumber() + " report was updated for your approval.",
        "/weekly-reports");
    auditLogService.record(
        AuditAction.WEEKLY_REPORT_SUBMITTED,
        "WEEKLY_REPORT",
        saved.getId(),
        "Weekly report for week " + saved.getWeekNumber() + " updated and re-submitted.");
    return WeeklyReportResponse.from(saved, studentName(studentId), attachments(saved.getId()));
  }

  /** Lists reports scoped to the caller: own (student), assigned (reviewers), or all (admin). */
  @Transactional(readOnly = true)
  public Page<WeeklyReportResponse> list(
      PrincipalRole role, UUID appUserId, UUID supervisorId, UUID academicStaffId, Pageable pageable) {
    Page<WeeklyReport> page = reportsByScope(role, appUserId, supervisorId, academicStaffId, pageable);
    Map<UUID, String> names = studentNames(page.getContent());
    Map<UUID, List<AttachmentInfo>> attachmentMap =
        attachmentService.listByOwners(
            AttachmentOwnerType.WEEKLY_REPORT,
            page.getContent().stream().map(WeeklyReport::getId).collect(Collectors.toList()));
    return page.map(
        r -> WeeklyReportResponse.from(r, names.get(r.getStudentId()), attachmentMap.get(r.getId())));
  }

  private Page<WeeklyReport> reportsByScope(
      PrincipalRole role, UUID appUserId, UUID supervisorId, UUID academicStaffId, Pageable pageable) {
    return switch (role) {
      case STUDENT -> weeklyReportRepository.findAllByStudentIdOrderByCreatedAtDesc(
          dataScope.studentId(appUserId), pageable);
      case SUPERVISOR -> {
        UUID target = supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
        yield weeklyReportRepository.findByStudentIdInOrderByCreatedAtDesc(
            internshipRepository.findStudentIdsBySupervisorId(target), pageable);
      }
      case FACULTY -> {
        Set<UUID> students =
            academicStaffId != null
                ? internshipRepository.findStudentIdsByAcademicStaffId(academicStaffId)
                : dataScope.assignedAcademicStudentIds(appUserId);
        yield weeklyReportRepository.findByStudentIdInOrderByCreatedAtDesc(students, pageable);
      }
      case ADMIN -> weeklyReportRepository.findAllByOrderByCreatedAtDesc(pageable);
    };
  }

  /** Fetch a single report, applying the same role scoping as {@link #list}. */
  @Transactional(readOnly = true)
  public WeeklyReportResponse get(UUID id, PrincipalRole role, UUID appUserId) {
    WeeklyReport report = find(id);
    if (!canView(report, role, appUserId)) {
      throw new ResourceNotFoundException(
          WEEKLY_REPORT_NOT_FOUND, "Weekly report not found: " + id);
    }
    return WeeklyReportResponse.from(
        report, studentName(report.getStudentId()), attachments(report.getId()));
  }

  /** Stage one: the company supervisor (or an admin) decides on a PENDING_COMPANY report. */
  @Transactional
  public WeeklyReportResponse companyReview(
      UUID id, PrincipalRole role, UUID appUserId, WeeklyReportReviewRequest request) {
    WeeklyReport report = find(id);
    if (report.getStatus() != WeeklyReportStatus.PENDING_COMPANY) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only freshly submitted weekly reports await the company decision: "
              + report.getStatus());
    }
    verifyCompanyReviewer(role, appUserId, report.getStudentId());
    report.applyCompanyReview(reviewerId(role, appUserId), request.approved(), request.comment());
    WeeklyReport saved = weeklyReportRepository.saveAndFlush(report);
    notifier.notify(
        studentAppUserId(saved.getStudentId()),
        request.approved() ? NotificationType.WEEKLY_REPORT_COMPANY_APPROVED
            : NotificationType.WEEKLY_REPORT_REJECTED,
        request.approved() ? "Weekly report approved" : "Weekly report rejected by the company",
        request.comment() == null ? null : request.comment(),
        "/weekly-reports");
    auditLogService.record(
        AuditAction.WEEKLY_REPORT_COMPANY_APPROVED,
        "WEEKLY_REPORT",
        saved.getId(),
        "Company " + (request.approved() ? "approved" : "rejected") + " week "
            + saved.getWeekNumber() + " report.");
    return response(saved);
  }

  /** Stage two: the academic staff (or an admin) decides on a COMPANY_APPROVED report. */
  @Transactional
  public WeeklyReportResponse academicReview(
      UUID id, PrincipalRole role, UUID appUserId, WeeklyReportReviewRequest request) {
    WeeklyReport report = find(id);
    if (report.getStatus() != WeeklyReportStatus.COMPANY_APPROVED) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only company-approved weekly reports await the academic decision: "
              + report.getStatus());
    }
    if (role == PrincipalRole.FACULTY
        && !dataScope.isAssignedToAcademicStudent(appUserId, report.getStudentId())) {
      throw new AccessDeniedException(
          "Student is not assigned to this academic staff member: " + report.getStudentId());
    }
    UUID reviewer = reviewerId(role, appUserId);
    if (Boolean.TRUE.equals(request.requestCorrection())) {
      report.applyCorrectionRequest(reviewer, request.comment());
      WeeklyReport corrected = weeklyReportRepository.saveAndFlush(report);
      notifier.notify(
          studentAppUserId(corrected.getStudentId()),
          NotificationType.WEEKLY_REPORT_CORRECTION_REQUESTED,
          "Weekly report corrections requested",
          request.comment() == null ? null : request.comment(),
          "/weekly-reports");
      auditLogService.record(
          AuditAction.WEEKLY_REPORT_REVIEWED,
          "WEEKLY_REPORT",
          corrected.getId(),
          "Academic staff requested corrections on week " + corrected.getWeekNumber() + " report.");
      return response(corrected);
    }
    report.applyAcademicReview(reviewer, request.approved(), request.comment());
    WeeklyReport saved = weeklyReportRepository.saveAndFlush(report);
    notifier.notify(
        studentAppUserId(saved.getStudentId()),
        request.approved() ? NotificationType.WEEKLY_REPORT_APPROVED
            : NotificationType.WEEKLY_REPORT_REJECTED,
        request.approved() ? "Weekly report fully approved"
            : "Weekly report rejected by the academic staff",
        request.comment() == null ? null : request.comment(),
        "/weekly-reports");
    auditLogService.record(
        AuditAction.WEEKLY_REPORT_REVIEWED,
        "WEEKLY_REPORT",
        saved.getId(),
        "Academic staff " + (request.approved() ? "approved" : "rejected") + " week "
            + saved.getWeekNumber() + " report.");
    return response(saved);
  }

  private WeeklyReportResponse response(WeeklyReport report) {
    return WeeklyReportResponse.from(
        report, studentName(report.getStudentId()), attachments(report.getId()));
  }

  private boolean canView(WeeklyReport report, PrincipalRole role, UUID appUserId) {
    return switch (role) {
      case STUDENT -> report.getStudentId().equals(dataScope.studentId(appUserId));
      case SUPERVISOR -> dataScope
          .assignedStudentIds(appUserId)
          .contains(report.getStudentId());
      case FACULTY -> dataScope
          .assignedAcademicStudentIds(appUserId)
          .contains(report.getStudentId());
      case ADMIN -> true;
    };
  }

  private void verifyCompanyReviewer(PrincipalRole role, UUID appUserId, UUID studentId) {
    if (role == PrincipalRole.ADMIN) {
      return;
    }
    if (role == PrincipalRole.SUPERVISOR) {
      if (!dataScope.isAssignedToStudent(appUserId, studentId)) {
        throw new AccessDeniedException(
            "Student is not assigned to this supervisor: " + studentId);
      }
      return;
    }
    throw new AccessDeniedException("Only company supervisors or admins review this stage.");
  }

  private UUID reviewerId(PrincipalRole role, UUID appUserId) {
    return switch (role) {
      case SUPERVISOR -> dataScope.supervisorId(appUserId);
      case FACULTY -> dataScope.academicStaffId(appUserId);
      default -> null;
    };
  }

  private WeeklyReport find(UUID id) {
    return weeklyReportRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    WEEKLY_REPORT_NOT_FOUND, "Weekly report not found: " + id));
  }

  private String studentName(UUID studentId) {
    if (studentId == null) {
      return null;
    }
    return studentRepository.findById(studentId).map(Student::getFullName).orElse(null);
  }

  private Map<UUID, String> studentNames(List<WeeklyReport> reports) {
    Set<UUID> ids =
        reports.stream().map(WeeklyReport::getStudentId).collect(Collectors.toSet());
    return studentRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(Student::getId, Student::getFullName));
  }

  private List<AttachmentInfo> attachments(UUID reportId) {
    return attachmentService.list(AttachmentOwnerType.WEEKLY_REPORT, reportId);
  }

  private UUID companySupervisorAppUserId(UUID internshipId) {
    if (internshipId == null) {
      return null;
    }
    return internshipRepository
        .findById(internshipId)
        .map(Internship::getSupervisorId)
        .flatMap(supervisorRepository::findById)
        .map(Supervisor::getAppUserId)
        .orElse(null);
  }

  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }
}