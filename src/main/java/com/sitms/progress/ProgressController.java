package com.sitms.progress;

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
 * Combined progress endpoints (SITMS-033).
 *
 * <p>Reads require an authenticated user (any role), matching the existing task progress endpoint.
 * Progress is role-scoped server-side: STUDENT sees only their own; SUPERVISOR their assigned
 * students; ADMIN any.
 */
@RestController
@RequestMapping("/progress")
public class ProgressController {

  private final ProgressService progressService;

  public ProgressController(ProgressService progressService) {
    this.progressService = progressService;
  }

  @GetMapping
  public ApiResponse<StudentProgress> progress(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID internshipId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        progressService.computeFor(
            role, appUserId, internshipId, studentId, supervisorId, academicStaffId));
  }
}
