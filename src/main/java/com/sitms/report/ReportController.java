package com.sitms.report;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Report generation endpoints (SITMS-044).
 *
 * <p>Report payloads are computed on demand from live data. Admins see all records; supervisors
 * are scoped to their assigned students (internship/activity reports) or the evaluations they
 * authored; faculty (university supervisors) to the students assigned to them academically; students
 * to their own internship, activity and evaluation records.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

  /** Every authenticated role may read reports; scoping happens in {@link ReportService}. */
  public static final String REPORTS_ACCESS = "hasAnyRole('ADMIN', 'SUPERVISOR', 'FACULTY', 'STUDENT')";

  private final ReportService reportService;

  public ReportController(ReportService reportService) {
    this.reportService = reportService;
  }

  @GetMapping("/internships")
  @PreAuthorize(REPORTS_ACCESS)
  public ApiResponse<InternshipReport> internshipReport(
      Authentication authentication,
      @RequestParam(required = false) UUID studentId) {
    Principal principal = principal(authentication);
    return ApiResponse.ok(
        reportService.internshipReport(principal.role(), principal.appUserId(), studentId));
  }

  @GetMapping("/activities")
  @PreAuthorize(REPORTS_ACCESS)
  public ApiResponse<ActivityReport> activityReport(
      Authentication authentication, @RequestParam(required = false) UUID studentId) {
    Principal principal = principal(authentication);
    return ApiResponse.ok(
        reportService.activityReport(principal.role(), principal.appUserId(), studentId));
  }

  @GetMapping("/evaluations")
  @PreAuthorize(REPORTS_ACCESS)
  public ApiResponse<EvaluationReport> evaluationReport(
      Authentication authentication,
      @RequestParam(required = false) UUID studentId) {
    Principal principal = principal(authentication);
    return ApiResponse.ok(
        reportService.evaluationReport(principal.role(), principal.appUserId(), studentId));
  }

  private static Principal principal(Authentication authentication) {
    String role =
        authentication.getAuthorities().stream()
            .map(Object::toString)
            .map(a -> a.replace("ROLE_", ""))
            .filter(a -> a.equals("ADMIN") || a.equals("SUPERVISOR") || a.equals("FACULTY") || a.equals("STUDENT"))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        ReportService.SUPERVISOR_PROFILE_NOT_FOUND,
                        "Unrecognized principal role."));
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return new Principal(role, appUserId);
  }

  private record Principal(String role, UUID appUserId) {}
}