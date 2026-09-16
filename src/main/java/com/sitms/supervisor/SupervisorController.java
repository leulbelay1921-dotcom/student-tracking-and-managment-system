package com.sitms.supervisor;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Supervisor CRUD endpoints (SITMS-015).
 *
 * <p>Reads require an authenticated user (any role); writes are ADMIN-only via
 * {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/supervisors")
public class SupervisorController {

  private final SupervisorService supervisorService;

  public SupervisorController(SupervisorService supervisorService) {
    this.supervisorService = supervisorService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<SupervisorResponse>> list(
      @RequestParam(required = false) UUID organizationId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            supervisorService.list(organizationId, active, department, q, pageable)));
  }

  @GetMapping("/departments")
  public ApiResponse<ApiResponse.PageData<String>> departments(
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(ApiResponse.PageData.from(supervisorService.departments(pageable)));
  }

  /** The calling supervisor's own profile, resolved from the JWT subject. */
  @GetMapping("/me")
  public ApiResponse<SupervisorResponse> me(
      org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
          token) {
    return ApiResponse.ok(supervisorService.me(PrincipalResolver.subject(token)));
  }

  @GetMapping("/{id}")
  public ApiResponse<SupervisorResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(supervisorService.get(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<SupervisorResponse> create(@Valid @RequestBody SupervisorRequest request) {
    return ApiResponse.ok("Supervisor created.", supervisorService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<SupervisorResponse> update(
      @PathVariable UUID id, @Valid @RequestBody SupervisorRequest request) {
    return ApiResponse.ok("Supervisor updated.", supervisorService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    supervisorService.delete(id);
    return ApiResponse.ok("Supervisor deleted.", null);
  }
}
