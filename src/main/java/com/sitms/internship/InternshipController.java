package com.sitms.internship;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internship CRUD + status lifecycle endpoints (SITMS-019).
 *
 * <p>Reads require an authenticated user (any role); create/update/delete are ADMIN-only; status
 * transitions are available to ADMIN and SUPERVISOR.
 */
@RestController
@RequestMapping("/internships")
public class InternshipController {

  private final InternshipService internshipService;

  public InternshipController(InternshipService internshipService) {
    this.internshipService = internshipService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<InternshipResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID organizationId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) InternshipStatus status,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            internshipService.list(
                role,
                appUserId,
                studentId,
                organizationId,
                supervisorId,
                academicStaffId,
                status,
                q,
                pageable)));
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<StudentInternshipResponse> me(JwtAuthenticationToken token) {
    Optional<StudentInternshipResponse> mine =
        internshipService.myInternship(PrincipalResolver.subject(token));
    return mine
        .map(ApiResponse::ok)
        .orElseGet(() -> ApiResponse.ok("No internship assigned yet.", null));
  }

  @GetMapping("/{id}")
  public ApiResponse<InternshipResponse> get(JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(internshipService.get(id, role, appUserId));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<InternshipResponse> create(@Valid @RequestBody InternshipRequest request) {
    return ApiResponse.ok("Internship created.", internshipService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<InternshipResponse> update(
      @PathVariable UUID id, @Valid @RequestBody InternshipRequest request) {
    return ApiResponse.ok("Internship updated.", internshipService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    internshipService.delete(id);
    return ApiResponse.ok("Internship deleted.", null);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<InternshipResponse> changeStatus(
      @PathVariable UUID id, @Valid @RequestBody InternshipStatusChangeRequest request) {
    return ApiResponse.ok(
        "Internship status updated.", internshipService.changeStatus(id, request.status()));
  }

  @PatchMapping("/{id}/supervisors")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<InternshipResponse> assignSupervisors(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody InternshipSupervisorAssignmentRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        "Supervisor(s) assigned.",
        internshipService.assignSupervisors(
            id, request.supervisorId(), request.academicStaffId(), role, appUserId));
  }
}
