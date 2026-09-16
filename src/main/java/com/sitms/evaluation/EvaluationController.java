package com.sitms.evaluation;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
 * Evaluation CRUD endpoints (SITMS-041).
 *
 * <p>Readers are any authenticated user but service-level scoping keeps students/supervisors on
 * their own data; writers are ADMIN or SUPERVISOR; deletion is ADMIN-only. Supervisors must
 * evaluate students assigned to them (enforced in {@link EvaluationService}).
 */
@RestController
@RequestMapping("/evaluations")
public class EvaluationController {

  private final EvaluationService evaluationService;

  public EvaluationController(EvaluationService evaluationService) {
    this.evaluationService = evaluationService;
  }

  @GetMapping
  @PreAuthorize(
      "hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('STUDENT') or hasRole('FACULTY')")
  public ApiResponse<ApiResponse.PageData<EvaluationResponse>> list(
      Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        ApiResponse.PageData.from(evaluationService.list(role, appUserId, pageable)));
  }

  @GetMapping("/assignees")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<ApiResponse.PageData<EvaluationAssigneeResponse>> assignees(
      Authentication authentication,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @PageableDefault(size = 20) Pageable pageable) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            evaluationService.listAssignees(role, appUserId, supervisorId, academicStaffId, pageable)));
  }

  @GetMapping("/{id}")
  @PreAuthorize(
      "hasRole('ADMIN') or hasRole('SUPERVISOR') or hasRole('STUDENT') or hasRole('FACULTY')")
  public ApiResponse<EvaluationResponse> get(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(evaluationService.get(id, role, appUserId, supervisorId, academicStaffId));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<EvaluationResponse> create(
      Authentication authentication, @Valid @RequestBody EvaluationRequest request) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        "Evaluation created.", evaluationService.create(request, role, appUserId));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<EvaluationResponse> update(
      Authentication authentication,
      @PathVariable UUID id,
      @Valid @RequestBody EvaluationRequest request) {
    String role = principalRole(authentication);
    UUID appUserId = "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        "Evaluation updated.", evaluationService.update(id, request, role, appUserId));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    evaluationService.delete(id);
    return ApiResponse.ok("Evaluation deleted.", null);
  }

  private static String principalRole(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(Object::toString)
        .map(a -> a.replace("ROLE_", ""))
        .filter(a -> a.equals("ADMIN") || a.equals("STUDENT") || a.equals("SUPERVISOR") || a.equals("FACULTY"))
        .findFirst()
        .orElseThrow(
            () ->
                new BusinessException(
                    EvaluationService.SUPERVISOR_PROFILE_NOT_FOUND,
                    "Unrecognized principal role."));
  }
}