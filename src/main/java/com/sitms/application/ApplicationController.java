package com.sitms.application;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Application endpoints (SITMS M2): reads for any authenticated role, student submission and
 * withdrawal, and ADMIN/SUPERVISOR decisions.
 */
@RestController
@RequestMapping("/applications")
public class ApplicationController {

  private final ApplicationService applicationService;

  public ApplicationController(ApplicationService applicationService) {
    this.applicationService = applicationService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<ApplicationResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID organizationId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) ApplicationStatus status,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            applicationService.list(
                role, appUserId, studentId, organizationId, supervisorId, academicStaffId, status, q, pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<ApplicationResponse> get(
      JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(applicationService.get(id, role, appUserId));
  }

  @PostMapping
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<ApplicationResponse> apply(
      Authentication authentication, @Valid @RequestBody ApplicationRequest request) {
    UUID appUserId = UUID.fromString(authentication.getName());
    return ApiResponse.ok("Internship application submitted.", applicationService.apply(appUserId, request));
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<ApplicationResponse> decide(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody ApplicationStatusChangeRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        "Application decided.", applicationService.decide(id, request.status(), role, appUserId));
  }

  @PostMapping("/{id}/withdraw")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<ApplicationResponse> withdraw(
      Authentication authentication, @PathVariable UUID id) {
    UUID appUserId = UUID.fromString(authentication.getName());
    return ApiResponse.ok("Application withdrawn.", applicationService.withdraw(id, appUserId));
  }
}