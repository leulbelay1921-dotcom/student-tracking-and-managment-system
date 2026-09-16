package com.sitms.activity;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Activity CRUD + submit/review workflow endpoints (SITMS-028).
 *
 * <p>Reads require an authenticated user (any role); update/delete are ADMIN-only; create is ADMIN or
 * STUDENT (self-service); submit is STUDENT-only (self-service); review is ADMIN, SUPERVISOR
 * (company stage) or FACULTY (university stage); the review-queue is ADMIN, SUPERVISOR or FACULTY.
 */
@RestController
@RequestMapping("/activities")
public class ActivityController {

  private final ActivityService activityService;

  public ActivityController(ActivityService activityService) {
    this.activityService = activityService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<ActivityResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID internshipId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) ActivityStatus status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            activityService.list(
                role, appUserId, studentId, internshipId, supervisorId, academicStaffId, status, from, to, q, pageable)));
  }

  @GetMapping("/review-queue")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<ApiResponse.PageData<ActivityResponse>> reviewQueue(
      JwtAuthenticationToken token,
      @RequestParam(defaultValue = "SUBMITTED") ActivityStatus status,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) UUID studentId,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            activityService.reviewQueue(role, appUserId, status, supervisorId, academicStaffId, studentId, pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<ActivityResponse> get(JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(activityService.get(id, role, appUserId));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
  public ApiResponse<ActivityResponse> create(
      Authentication authentication, @Valid @RequestBody ActivityRequest request) {
    boolean selfService =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
    UUID appUserId =
        selfService ? UUID.fromString(authentication.getName()) : null;
    return ApiResponse.ok(
        "Activity logged.", activityService.create(request, appUserId, selfService));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
  public ApiResponse<ActivityResponse> update(
      Authentication authentication, @PathVariable UUID id, @Valid @RequestBody ActivityRequest request) {
    boolean selfService =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
    UUID appUserId =
        selfService ? UUID.fromString(authentication.getName()) : null;
    return ApiResponse.ok("Activity updated.", activityService.update(id, request, appUserId, selfService));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    activityService.delete(id);
    return ApiResponse.ok("Activity deleted.", null);
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<ActivityResponse> submit(
      Authentication authentication, @PathVariable UUID id) {
    UUID appUserId = UUID.fromString(authentication.getName());
    return ApiResponse.ok("Activity submitted for review.", activityService.submit(id, appUserId));
  }

  /** Reviews an activity at the company (SUBMITTED) or university (COMPANY_APPROVED) stage. */
  @PostMapping("/{id}/review")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<ActivityResponse> review(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody ActivityReviewRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok("Activity reviewed.", activityService.review(id, role, appUserId, request));
  }
}
