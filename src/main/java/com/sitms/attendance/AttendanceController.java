package com.sitms.attendance;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Attendance tracking endpoints (SITMS-070).
 *
 * <p>Reads are role-scoped server-side: STUDENT sees their own; SUPERVISOR and FACULTY their
 * assigned students; ADMIN may review any. Students self check-in/out, submit leave requests and
 * report incorrect attendance; their supervisors (or admins) decide those requests.
 */
@RestController
@RequestMapping("/attendance")
public class AttendanceController {

  private final AttendanceService attendanceService;

  public AttendanceController(AttendanceService attendanceService) {
    this.attendanceService = attendanceService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<AttendanceResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            attendanceService.list(role, appUserId, studentId, supervisorId, academicStaffId, from, to, pageable)));
  }

  @GetMapping("/summary")
  public ApiResponse<AttendanceSummary> summary(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(attendanceService.summary(role, appUserId, studentId, supervisorId, academicStaffId));
  }

  /** Records a day of PRESENT/ABSENT attendance for a student assigned to the calling supervisor. */
  @PostMapping("/record")
  @PreAuthorize("hasRole('SUPERVISOR')")
  public ApiResponse<AttendanceResponse> record(
      JwtAuthenticationToken token, @Valid @RequestBody AttendanceRecordRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Attendance recorded.",
        attendanceService.record(role, appUserId, request.studentId(), request));
  }

  /** Student self check-in; a check-in after 09:00 is marked LATE. */
  @PostMapping("/check-in")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AttendanceResponse> checkIn(
      JwtAuthenticationToken token, @RequestBody(required = false) AttendanceCheckInRequest request) {
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Checked in.",
        attendanceService.checkIn(
            appUserId,
            request == null ? null : request.internshipId(),
            request == null ? null : request.date()));
  }

  /** Student self check-out; requires a check-in for the same date. */
  @PostMapping("/check-out")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AttendanceResponse> checkOut(
      JwtAuthenticationToken token, @RequestBody(required = false) AttendanceCheckOutRequest request) {
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Checked out.", attendanceService.checkOut(appUserId, request == null ? null : request.date()));
  }

  /** Corrects an attendance record (status, check-in/out times, note) for an assigned student. */
  @PutMapping("/correct")
  @PreAuthorize("hasRole('SUPERVISOR')")
  public ApiResponse<AttendanceResponse> correct(
      JwtAuthenticationToken token, @Valid @RequestBody AttendanceCorrectionRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Attendance corrected.", attendanceService.correctAttendance(role, appUserId, request));
  }

  @GetMapping("/leave-requests")
  public ApiResponse<ApiResponse.PageData<LeaveRequestResponse>> leaveRequests(
      JwtAuthenticationToken token, @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(attendanceService.listLeaveRequests(role, appUserId, pageable)));
  }

  /** A student submits a leave request for one or more dates. */
  @PostMapping("/leave-requests")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<LeaveRequestResponse> submitLeave(
      JwtAuthenticationToken token, @Valid @RequestBody LeaveRequestRequest request) {
    return ApiResponse.ok(
        "Leave requested.", attendanceService.submitLeave(PrincipalResolver.subject(token), request));
  }

  /** Decides a pending leave request (assigned supervisor or admin). */
  @PostMapping("/leave-requests/{id}/review")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<LeaveRequestResponse> reviewLeave(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody AttendanceRequestReviewRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        "Leave request reviewed.", attendanceService.reviewLeave(id, role, appUserId, request));
  }

  @GetMapping("/issues")
  public ApiResponse<ApiResponse.PageData<AttendanceIssueResponse>> issues(
      JwtAuthenticationToken token, @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(attendanceService.listIssues(role, appUserId, pageable)));
  }

  /** A student reports incorrect attendance for one of their own rows. */
  @PostMapping("/issues")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<AttendanceIssueResponse> reportIssue(
      JwtAuthenticationToken token, @Valid @RequestBody AttendanceIssueRequest request) {
    return ApiResponse.ok(
        "Attendance issue reported.",
        attendanceService.reportIssue(PrincipalResolver.subject(token), request));
  }

  /** Responds to a pending attendance issue; on approval carries an optional correction over. */
  @PostMapping("/issues/{id}/review")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<AttendanceIssueResponse> reviewIssue(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody AttendanceIssueReviewRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        "Attendance issue reviewed.", attendanceService.reviewIssue(id, role, appUserId, request));
  }
}