package com.sitms.analytics;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Aggregate analytics endpoints (SITMS-035). Admin/supervisor scope: status distribution, per-program
 * (department) stats, and weekly activity trends. Supervisors are scoped to their assigned students.
 */
@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

  private final AnalyticsService analyticsService;

  public AnalyticsController(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<AnalyticsSummary> summary(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(analyticsService.summary(role, appUserId, studentId));
  }
}
