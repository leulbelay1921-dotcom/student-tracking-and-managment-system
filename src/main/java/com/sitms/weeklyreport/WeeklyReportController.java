package com.sitms.weeklyreport;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
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
 * Weekly report endpoints (SITMS-077).
 *
 * <p>Students create/edit their weekly reports and attach documents; reports then flow through a
 * two-stage approval: the company supervisor decides first, after which the academic staff gives
 * the final verdict. Reads are role-scoped server-side.
 */
@RestController
@RequestMapping("/weekly-reports")
public class WeeklyReportController {

  private final WeeklyReportService weeklyReportService;

  public WeeklyReportController(WeeklyReportService weeklyReportService) {
    this.weeklyReportService = weeklyReportService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<WeeklyReportResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            weeklyReportService.list(role, appUserId, supervisorId, academicStaffId, pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<WeeklyReportResponse> get(
      JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(weeklyReportService.get(id, role, appUserId));
  }

  @PostMapping
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<WeeklyReportResponse> create(
      JwtAuthenticationToken token, @Valid @RequestBody WeeklyReportRequest request) {
    return ApiResponse.ok(
        "Weekly report submitted.",
        weeklyReportService.create(PrincipalResolver.subject(token), request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<WeeklyReportResponse> update(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody WeeklyReportRequest request) {
    return ApiResponse.ok(
        "Weekly report updated.",
        weeklyReportService.update(PrincipalResolver.subject(token), id, request));
  }

  /** Stage one: the company supervisor (or an admin) decides on a submitted report. */
  @PostMapping("/{id}/company-review")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<WeeklyReportResponse> companyReview(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody WeeklyReportReviewRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        "Company review recorded.",
        weeklyReportService.companyReview(id, role, appUserId, request));
  }

  /** Stage two: the academic staff (or an admin) gives the final verdict. */
  @PostMapping("/{id}/academic-review")
  @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
  public ApiResponse<WeeklyReportResponse> academicReview(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody WeeklyReportReviewRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        "Academic review recorded.",
        weeklyReportService.academicReview(id, role, appUserId, request));
  }
}