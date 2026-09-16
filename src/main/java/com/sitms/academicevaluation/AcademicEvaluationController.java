package com.sitms.academicevaluation;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Academic evaluation endpoints (SITMS-078).
 *
 * <p>Readers are ADMIN, FACULTY or STUDENT with service-level scoping keeping students and academic
 * supervisors on their own data; writers are ADMIN or FACULTY. Academic supervisors must evaluate
 * internships assigned to them (enforced in {@link AcademicEvaluationService}). Saving is an upsert
 * keyed by internship: an internship has at most one academic evaluation.
 */
@RestController
@RequestMapping("/academic-evaluations")
public class AcademicEvaluationController {

  private final AcademicEvaluationService academicEvaluationService;

  public AcademicEvaluationController(AcademicEvaluationService academicEvaluationService) {
    this.academicEvaluationService = academicEvaluationService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY') or hasRole('STUDENT')")
  public ApiResponse<ApiResponse.PageData<AcademicEvaluationResponse>> list(
      Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        ApiResponse.PageData.from(academicEvaluationService.list(role, appUserId, pageable)));
  }

  @GetMapping("/assignees")
  @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
  public ApiResponse<ApiResponse.PageData<AcademicEvaluationAssigneeResponse>> assignees(
      Authentication authentication,
      @RequestParam(required = false) UUID academicStaffId,
      @PageableDefault(size = 20) Pageable pageable) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            academicEvaluationService.listAssignees(role, appUserId, academicStaffId, pageable)));
  }

  @GetMapping("/by-internship/{internshipId}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY') or hasRole('STUDENT')")
  public ApiResponse<AcademicEvaluationResponse> byInternship(
      Authentication authentication, @PathVariable UUID internshipId) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        academicEvaluationService.byInternship(internshipId, role, appUserId));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY') or hasRole('STUDENT')")
  public ApiResponse<AcademicEvaluationResponse> get(
      Authentication authentication, @PathVariable UUID id) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(academicEvaluationService.get(id, role, appUserId));
  }

  /** Save (create or update) the academic evaluation for an internship. */
  @PutMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('FACULTY')")
  public ApiResponse<AcademicEvaluationResponse> save(
      Authentication authentication,
      @Valid @RequestBody AcademicEvaluationRequest request) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        "Academic evaluation saved.", academicEvaluationService.upsert(request, role, appUserId));
  }

  private static String principalRole(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(Object::toString)
        .map(a -> a.replace("ROLE_", ""))
        .filter(a -> a.equals("ADMIN") || a.equals("STUDENT") || a.equals("FACULTY"))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    AcademicEvaluationService.ACADEMIC_STAFF_PROFILE_NOT_FOUND,
                    "Unrecognized principal role."));
  }
}