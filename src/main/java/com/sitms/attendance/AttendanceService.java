package com.sitms.attendance;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.internship.Internship;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.Supervisor;
import com.sitms.supervisor.SupervisorRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link Attendance} tracking (SITMS-070). */
@Service
public class AttendanceService {

  public static final String ATTENDANCE_STUDENT_REQUIRED = "ATTENDANCE_STUDENT_REQUIRED";
  public static final String ATTENDANCE_NOT_FOUND = "ATTENDANCE_NOT_FOUND";
  public static final String LEAVE_REQUEST_NOT_FOUND = "LEAVE_REQUEST_NOT_FOUND";
  public static final String ATTENDANCE_ISSUE_NOT_FOUND = "ATTENDANCE_ISSUE_NOT_FOUND";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  public static final String LEAVE_OVERLAP = "LEAVE_OVERLAP";
  public static final String NO_CHECK_IN = "NO_CHECK_IN";
  public static final String INVALID_DATE_RANGE = "INVALID_DATE_RANGE";

  /** A check-in after this time is recorded as {@link AttendanceStatus#LATE}. */
  public static final LocalTime LATE_AFTER = LocalTime.of(9, 0);

  private final AttendanceRepository attendanceRepository;
  private final LeaveRequestRepository leaveRequestRepository;
  private final AttendanceIssueRepository attendanceIssueRepository;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final com.sitms.internship.InternshipRepository internshipRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;
  private final DataScope dataScope;

  public AttendanceService(
      AttendanceRepository attendanceRepository,
      LeaveRequestRepository leaveRequestRepository,
      AttendanceIssueRepository attendanceIssueRepository,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      com.sitms.internship.InternshipRepository internshipRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService,
      DataScope dataScope) {
    this.attendanceRepository = attendanceRepository;
    this.leaveRequestRepository = leaveRequestRepository;
    this.attendanceIssueRepository = attendanceIssueRepository;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.internshipRepository = internshipRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
    this.dataScope = dataScope;
  }

  /**
   * Lists attendance scoped to the calling principal. SUPERVISOR and FACULTY see their assigned
  * students; STUDENT sees their own; ADMIN may review any student. Cross-scope
   * requests are silently empty.
   */
  @Transactional(readOnly = true)
  public Page<AttendanceResponse> list(
      PrincipalRole role, UUID appUserId, UUID studentId, UUID supervisorId, UUID academicStaffId,
      LocalDate from, LocalDate to, Pageable pageable) {
    UUID scopeStudentId = studentId;
    UUID scopeSupervisorId = null;
    UUID scopeAcademicStaffId = null;
    switch (role) {
      case STUDENT -> {
        UUID own = dataScope.studentId(appUserId);
        scopeStudentId = own;
        if (studentId != null && !studentId.equals(own)) {
          return Page.empty(pageable);
        }
      }
      case SUPERVISOR -> {
        scopeSupervisorId =
            supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
        if (studentId != null
            && !internshipRepository.existsBySupervisorIdAndStudentId(
                scopeSupervisorId, studentId)) {
          return Page.empty(pageable);
        }
      }
      case FACULTY -> {
        if (academicStaffId != null) {
          scopeAcademicStaffId = academicStaffId;
          if (studentId != null
              && !internshipRepository.existsByAcademicStaffIdAndStudentId(
                  academicStaffId, studentId)) {
            return Page.empty(pageable);
          }
        } else {
          scopeAcademicStaffId = dataScope.academicStaffId(appUserId);
          if (studentId != null
              && !dataScope.isAssignedToAcademicStudent(appUserId, studentId)) {
            return Page.empty(pageable);
          }
        }
      }
      case ADMIN -> {
      }
    }
    return attendanceRepository
        .findAll(filters(scopeStudentId, scopeSupervisorId, scopeAcademicStaffId, from, to), pageable)
        .map(AttendanceResponse::from);
  }

  /**
   * Aggregates attendance for one student, verifying the principal may view it. STUDENT sees only
   * their own; SUPERVISOR and FACULTY only assigned students. ADMIN is denied. Cross-scope access
   * yields 403.
   */
  @Transactional(readOnly = true)
  public AttendanceSummary summary(
      PrincipalRole role, UUID appUserId, UUID studentId, UUID supervisorId, UUID academicStaffId) {
    UUID target =
        switch (role) {
          case STUDENT -> {
            UUID own = dataScope.studentId(appUserId);
            if (studentId != null && !studentId.equals(own)) {
              throw new AccessDeniedException(
                  "Attendance is only available for your own profile.");
            }
            yield own;
          }
          case SUPERVISOR -> {
            if (studentId == null) {
              throw new BusinessException(
                  ATTENDANCE_STUDENT_REQUIRED, "A studentId is required for supervisor summaries.");
            }
            UUID targetSupervisorId =
                supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
            if (!internshipRepository.existsBySupervisorIdAndStudentId(
                targetSupervisorId, studentId)) {
              throw new AccessDeniedException(
                  "Student is not assigned to this supervisor: " + studentId);
            }
            yield studentId;
          }
          case FACULTY -> {
            if (studentId == null) {
              throw new BusinessException(
                  ATTENDANCE_STUDENT_REQUIRED,
                  "A studentId is required for faculty summaries.");
            }
            if (academicStaffId != null) {
              if (!internshipRepository.existsByAcademicStaffIdAndStudentId(
                  academicStaffId, studentId)) {
                throw new AccessDeniedException(
                    "Student is not assigned to this academic staff member: " + studentId);
              }
            } else if (!dataScope.isAssignedToAcademicStudent(appUserId, studentId)) {
              throw new AccessDeniedException(
                  "Student is not assigned to this academic staff member: " + studentId);
            }
            yield studentId;
          }
          case ADMIN -> {
            if (studentId == null) {
              throw new BusinessException(
                  ATTENDANCE_STUDENT_REQUIRED, "A studentId is required for administrator summaries.");
            }
            yield studentId;
          }
        };
    long presentDays = attendanceRepository.countByStudentIdAndStatus(target, AttendanceStatus.PRESENT);
    long lateDays = attendanceRepository.countByStudentIdAndStatus(target, AttendanceStatus.LATE);
    long absentDays = attendanceRepository.countByStudentIdAndStatus(target, AttendanceStatus.ABSENT);
    long permissionDays =
        attendanceRepository.countByStudentIdAndStatus(target, AttendanceStatus.PERMISSION);
    return summarize(target, presentDays, lateDays, absentDays, permissionDays);
  }

  /** Records a day of attendance for an assigned student (supervisor write). */
  @Transactional
  public AttendanceResponse record(
      PrincipalRole role,
      UUID appUserId,
      UUID studentId,
      AttendanceRecordRequest request) {
    if (role != PrincipalRole.SUPERVISOR) {
      throw new AccessDeniedException("Only supervisors can record attendance.");
    }
    if (studentId == null) {
      throw new BusinessException(ATTENDANCE_STUDENT_REQUIRED, "A studentId is required.");
    }
    if (!dataScope.isAssignedToStudent(appUserId, studentId)) {
      throw new AccessDeniedException("Student is not assigned to this supervisor: " + studentId);
    }
    LocalDate date = request.date() == null ? LocalDate.now() : request.date();
    Attendance attendance =
        attendanceRepository
            .findByStudentIdAndAttendanceDate(studentId, date)
            .orElseGet(() -> Attendance.builder().studentId(studentId).attendanceDate(date).build());
    attendance.record(request.status(), request.note());
    AttendanceResponse response = AttendanceResponse.from(attendanceRepository.saveAndFlush(attendance));
    auditLogService.record(
        AuditAction.ATTENDANCE_RECORDED,
        "ATTENDANCE",
        attendance.getId(),
        "Attendance recorded as " + request.status() + ".");
    return response;
  }

  /** Student self check-in: creates (or updates) today's row with the current time. A check-in
   * after {@value #LATE_AFTER} hour is marked late. */
  @Transactional
  public AttendanceResponse checkIn(UUID appUserId, UUID internshipId, LocalDate date) {
    UUID studentId = dataScope.studentId(appUserId);
    LocalDate day = date == null ? LocalDate.now() : date;
    Attendance attendance =
        attendanceRepository
            .findByStudentIdAndAttendanceDate(studentId, day)
            .orElseGet(
                () ->
                    Attendance.builder()
                        .studentId(studentId)
                        .internshipId(internshipId)
                        .attendanceDate(day)
                        .build());
    LocalTime now = LocalTime.now();
    AttendanceStatus status =
        now.isAfter(LATE_AFTER) ? AttendanceStatus.LATE : AttendanceStatus.PRESENT;
    attendance.checkIn(now, status);
    AttendanceResponse response =
        AttendanceResponse.from(attendanceRepository.saveAndFlush(attendance));
    auditLogService.record(
        AuditAction.ATTENDANCE_RECORDED,
        "ATTENDANCE",
        attendance.getId(),
        "Student checked in at " + now + " (" + status + ").");
    return response;
  }

  /** Student self check-out: records the current time on the student's row for the given date. */
  @Transactional
  public AttendanceResponse checkOut(UUID appUserId, LocalDate date) {
    UUID studentId = dataScope.studentId(appUserId);
    LocalDate day = date == null ? LocalDate.now() : date;
    Attendance attendance =
        attendanceRepository
            .findByStudentIdAndAttendanceDate(studentId, day)
            .orElseThrow(() -> new BusinessException(NO_CHECK_IN, "No check-in for " + day + "."));
    attendance.checkOut(LocalTime.now());
    AttendanceResponse response =
        AttendanceResponse.from(attendanceRepository.saveAndFlush(attendance));
    auditLogService.record(
        AuditAction.ATTENDANCE_RECORDED,
        "ATTENDANCE",
        attendance.getId(),
        "Student checked out at " + attendance.getCheckOut() + ".");
    return response;
  }

  /** Corrects an attendance row for an assigned student (supervisor write). The row is created for
   * the given date when it does not exist yet; only provided fields are overwritten. */
  @Transactional
  public AttendanceResponse correctAttendance(
      PrincipalRole role, UUID appUserId, AttendanceCorrectionRequest request) {
    if (role != PrincipalRole.SUPERVISOR) {
      throw new AccessDeniedException("Only supervisors can correct attendance.");
    }
    if (!dataScope.isAssignedToStudent(appUserId, request.studentId())) {
      throw new AccessDeniedException(
          "Student is not assigned to this supervisor: " + request.studentId());
    }
    Attendance attendance =
        attendanceRepository
            .findByStudentIdAndAttendanceDate(request.studentId(), request.date())
            .orElseGet(
                () ->
                    Attendance.builder()
                        .studentId(request.studentId())
                        .attendanceDate(request.date())
                        .build());
    attendance.correct(
        request.status(), request.checkIn(), request.checkOut(), request.note());
    AttendanceResponse response =
        AttendanceResponse.from(attendanceRepository.saveAndFlush(attendance));
    auditLogService.record(
        AuditAction.ATTENDANCE_RECORDED,
        "ATTENDANCE",
        attendance.getId(),
        "Attendance corrected for " + request.date() + ".");
    return response;
  }

  /** A student submits a leave request; overlapping pending requests are rejected. */
  @Transactional
  public LeaveRequestResponse submitLeave(UUID appUserId, LeaveRequestRequest request) {
    UUID studentId = dataScope.studentId(appUserId);
    if (request.endDate().isBefore(request.startDate())) {
      throw new BusinessException(INVALID_DATE_RANGE, "endDate must not be before startDate.");
    }
    if (leaveRequestRepository.overlapsPending(studentId, request.startDate(), request.endDate())) {
      throw new BusinessException(LEAVE_OVERLAP, "A pending leave request already covers these dates.");
    }
    LeaveRequest leaveRequest =
        leaveRequestRepository.saveAndFlush(
            LeaveRequest.builder()
                .studentId(studentId)
                .internshipId(request.internshipId())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .status(AttendanceRequestStatus.PENDING)
                .build());
    notifier.notify(
        supervisorAppUserId(leaveRequest.getInternshipId()),
        NotificationType.LEAVE_REQUEST_SUBMITTED,
        "Leave request awaiting approval",
        leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate(),
        "/attendance");
    auditLogService.record(
        AuditAction.LEAVE_REQUEST_SUBMITTED,
        "LEAVE_REQUEST",
        leaveRequest.getId(),
        "Leave requested for " + leaveRequest.getStartDate() + " to " + leaveRequest.getEndDate() + ".");
    return LeaveRequestResponse.from(leaveRequest);
  }

  /** Lists leave requests scoped to the calling principal (own / assigned / all). */
  @Transactional(readOnly = true)
  public Page<LeaveRequestResponse> listLeaveRequests(
      PrincipalRole role, UUID appUserId, Pageable pageable) {
    return leaveRequestsByScope(role, appUserId, pageable).map(LeaveRequestResponse::from);
  }

  private Page<LeaveRequest> leaveRequestsByScope(
      PrincipalRole role, UUID appUserId, Pageable pageable) {
    return switch (role) {
      case STUDENT -> leaveRequestRepository.findByStudentId(
          dataScope.studentId(appUserId), pageable);
      case SUPERVISOR -> leaveRequestRepository.findByStudentIdInOrderByCreatedAtDesc(
          dataScope.assignedStudentIds(appUserId), pageable);
      case FACULTY -> leaveRequestRepository.findByStudentIdInOrderByCreatedAtDesc(
          dataScope.assignedAcademicStudentIds(appUserId), pageable);
      case ADMIN -> leaveRequestRepository.findAll(pageable);
    };
  }

  /** Reviews a PENDING leave request. A supervisor may only review requests of assigned students. */
  @Transactional
  public LeaveRequestResponse reviewLeave(
      UUID id, PrincipalRole role, UUID appUserId, AttendanceRequestReviewRequest request) {
    LeaveRequest leaveRequest =
        leaveRequestRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        LEAVE_REQUEST_NOT_FOUND, "Leave request not found: " + id));
    if (leaveRequest.getStatus() != AttendanceRequestStatus.PENDING) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only pending leave requests can be reviewed: " + leaveRequest.getStatus());
    }
    UUID reviewerId = verifyAssignable(role, appUserId, leaveRequest.getStudentId());
    leaveRequest.updateReview(
        reviewerId, request.decision(), request.feedback() == null ? null : request.feedback());
    LeaveRequestResponse response =
        LeaveRequestResponse.from(leaveRequestRepository.saveAndFlush(leaveRequest));
    notifier.notify(
        studentAppUserId(leaveRequest.getStudentId()),
        NotificationType.LEAVE_REQUEST_REVIEWED,
        request.decision() == AttendanceRequestStatus.APPROVED ? "Leave approved" : "Leave rejected",
        request.feedback() == null ? null : request.feedback(),
        "/attendance");
    auditLogService.record(
        AuditAction.LEAVE_REQUEST_REVIEWED,
        "LEAVE_REQUEST",
        leaveRequest.getId(),
        "Leave request " + (request.decision() == AttendanceRequestStatus.APPROVED ? "approved" : "rejected") + ".");
    return response;
  }

  /** A student reports incorrect attendance for one of their own recorded rows. */
  @Transactional
  public AttendanceIssueResponse reportIssue(UUID appUserId, AttendanceIssueRequest request) {
    UUID studentId = dataScope.studentId(appUserId);
    Attendance attendance =
        attendanceRepository
            .findById(request.attendanceId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ATTENDANCE_NOT_FOUND, "Attendance not found: " + request.attendanceId()));
    if (!attendance.getStudentId().equals(studentId)) {
      throw new AccessDeniedException("Attendance does not belong to this student: " + request.attendanceId());
    }
    AttendanceIssue issue =
        attendanceIssueRepository.saveAndFlush(
            AttendanceIssue.builder()
                .attendanceId(attendance.getId())
                .studentId(studentId)
                .message(request.message())
                .status(AttendanceRequestStatus.PENDING)
                .build());
    notifier.notify(
        attendanceSupervisorAppUserId(attendance),
        NotificationType.ATTENDANCE_ISSUE_REPORTED,
        "Attendance issue reported",
        "A student reported incorrect attendance on " + attendance.getAttendanceDate() + ".",
        "/attendance");
    auditLogService.record(
        AuditAction.ATTENDANCE_ISSUE_REPORTED,
        "ATTENDANCE_ISSUE",
        issue.getId(),
        "Student reported incorrect attendance for " + attendance.getAttendanceDate() + ".");
    return AttendanceIssueResponse.from(issue);
  }

  /** Lists attendance issues scoped to the calling principal (own / assigned / all). */
  @Transactional(readOnly = true)
  public Page<AttendanceIssueResponse> listIssues(
      PrincipalRole role, UUID appUserId, Pageable pageable) {
    return issuesByScope(role, appUserId, pageable).map(AttendanceIssueResponse::from);
  }

  private Page<AttendanceIssue> issuesByScope(
      PrincipalRole role, UUID appUserId, Pageable pageable) {
    return switch (role) {
      case STUDENT -> attendanceIssueRepository.findByStudentId(
          dataScope.studentId(appUserId), pageable);
      case SUPERVISOR -> attendanceIssueRepository.findByStudentIdInOrderByCreatedAtDesc(
          dataScope.assignedStudentIds(appUserId), pageable);
      case FACULTY -> attendanceIssueRepository.findByStudentIdInOrderByCreatedAtDesc(
          dataScope.assignedAcademicStudentIds(appUserId), pageable);
      case ADMIN -> attendanceIssueRepository.findAll(pageable);
    };
  }

  /**
   * Reviews a PENDING attendance issue. On approval the reviewer may carry a correction over to the
   * attendance row. Only supervisors of the student's internship (or admins) may decide.
   */
  @Transactional
  public AttendanceIssueResponse reviewIssue(
      UUID id, PrincipalRole role, UUID appUserId, AttendanceIssueReviewRequest request) {
    AttendanceIssue issue =
        attendanceIssueRepository
            .findById(id)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ATTENDANCE_ISSUE_NOT_FOUND, "Attendance issue not found: " + id));
    if (issue.getStatus() != AttendanceRequestStatus.PENDING) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only pending attendance issues can be reviewed: " + issue.getStatus());
    }
    UUID reviewerId = verifyAssignable(role, appUserId, issue.getStudentId());
    if (request.decision() == AttendanceRequestStatus.APPROVED) {
      applyIssueCorrection(issue, request);
    }
    issue.updateReview(reviewerId, request.decision(), request.feedback());
    AttendanceIssueResponse response =
        AttendanceIssueResponse.from(attendanceIssueRepository.saveAndFlush(issue));
    notifier.notify(
        studentAppUserId(issue.getStudentId()),
        NotificationType.ATTENDANCE_ISSUE_REVIEWED,
        request.decision() == AttendanceRequestStatus.APPROVED ? "Attendance issue acknowledged"
            : "Attendance issue closed",
        request.feedback() == null ? null : request.feedback(),
        "/attendance");
    auditLogService.record(
        AuditAction.ATTENDANCE_ISSUE_REVIEWED,
        "ATTENDANCE_ISSUE",
        issue.getId(),
        request.decision() == AttendanceRequestStatus.APPROVED
            ? (request.correctedStatus() != null
                ? "Attendance issue approved and record corrected."
                : "Attendance issue acknowledged.")
            : "Attendance issue closed.");
    return response;
  }

  private void applyIssueCorrection(AttendanceIssue issue, AttendanceIssueReviewRequest request) {
    attendanceRepository
        .findById(issue.getAttendanceId())
        .ifPresent(attendance -> {
          if (request.correctedStatus() != null
              || request.correctedCheckIn() != null
              || request.correctedCheckOut() != null
              || request.correctedNote() != null) {
            attendance.correct(
                request.correctedStatus(),
                request.correctedCheckIn(),
                request.correctedCheckOut(),
                request.correctedNote());
            attendanceRepository.saveAndFlush(attendance);
            auditLogService.record(
                AuditAction.ATTENDANCE_RECORDED,
                "ATTENDANCE",
                attendance.getId(),
                "Attendance corrected after issue review.");
          }
        });
  }

  /** Admins review any student; supervisors and faculty only assigned students; returns the
   * reviewer's profile id (null for admins). */
  private UUID verifyAssignable(PrincipalRole role, UUID appUserId, UUID studentId) {
    if (role == PrincipalRole.ADMIN) {
      return null;
    }
    if (role == PrincipalRole.SUPERVISOR) {
      UUID supervisorId = dataScope.supervisorId(appUserId);
      if (!dataScope.isAssignedToStudent(appUserId, studentId)) {
        throw new AccessDeniedException("Student is not assigned to this supervisor: " + studentId);
      }
      return supervisorId;
    }
    if (role == PrincipalRole.FACULTY) {
      UUID academicStaffId = dataScope.academicStaffId(appUserId);
      if (!dataScope.isAssignedToAcademicStudent(appUserId, studentId)) {
        throw new AccessDeniedException("Student is not assigned to this academic staff: " + studentId);
      }
      return academicStaffId;
    }
throw new AccessDeniedException("Only reviewers can decide attendance requests.");
  }

  /** The app_user id of the supervisor of the internship that owns the attendance row, or null. */
  private UUID attendanceSupervisorAppUserId(Attendance attendance) {
    return supervisorAppUserId(attendance.getInternshipId());
  }

  /** The app_user id of the supervisor of an internship, or null when internshipId is unknown. */
  private UUID supervisorAppUserId(UUID internshipId) {
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

  /** The app_user id of the student profile, or null. */
  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Attendance> filters(
      UUID studentId,
      UUID supervisorId,
      UUID academicStaffId,
      LocalDate from,
      LocalDate to) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (studentId != null) {
        predicates.add(cb.equal(root.get("studentId"), studentId));
      }
      if (from != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("attendanceDate"), from));
      }
      if (to != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("attendanceDate"), to));
      }
      if (supervisorId != null) {
        jakarta.persistence.criteria.Subquery<UUID> sub = query.subquery(UUID.class);
        var internshipRoot = sub.from(Internship.class);
        sub.select(internshipRoot.get("studentId"))
            .where(cb.equal(internshipRoot.get("supervisorId"), supervisorId));
        predicates.add(root.get("studentId").in(sub));
      }
      if (academicStaffId != null) {
        jakarta.persistence.criteria.Subquery<UUID> sub = query.subquery(UUID.class);
        var internshipRoot = sub.from(Internship.class);
        sub.select(internshipRoot.get("studentId"))
            .where(cb.equal(internshipRoot.get("academicStaffId"), academicStaffId));
        predicates.add(root.get("studentId").in(sub));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static AttendanceSummary summarize(
      UUID studentId, long presentDays, long lateDays, long absentDays, long permissionDays) {
    long totalDays = presentDays + lateDays + absentDays + permissionDays;
    Double rate =
        totalDays == 0
            ? null
            : Math.round(
                    ((presentDays + lateDays + permissionDays) * 100.0) / totalDays * 10.0)
                / 10.0;
    return new AttendanceSummary(
        studentId, totalDays, presentDays, lateDays, absentDays, permissionDays, rate);
  }
}