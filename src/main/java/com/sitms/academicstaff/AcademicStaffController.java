package com.sitms.academicstaff;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Academic staff CRUD endpoints (SITMS-072).
 *
 * <p>Reads require an authenticated user (any role); writes are ADMIN-only via
 * {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/academic-staff")
public class AcademicStaffController {

  private final AcademicStaffService academicStaffService;

  public AcademicStaffController(AcademicStaffService academicStaffService) {
    this.academicStaffService = academicStaffService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<AcademicStaffResponse>> list(
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            academicStaffService.list(active, department, q, pageable)));
  }

  @GetMapping("/departments")
  public ApiResponse<ApiResponse.PageData<String>> departments(
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(ApiResponse.PageData.from(academicStaffService.departments(pageable)));
  }

  /** The calling academic staff member's own profile, resolved from the JWT subject. */
  @GetMapping("/me")
  public ApiResponse<AcademicStaffResponse> me(JwtAuthenticationToken token) {
    return ApiResponse.ok(academicStaffService.byAppUserId(PrincipalResolver.subject(token)));
  }

  @GetMapping("/{id}")
  public ApiResponse<AcademicStaffResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(academicStaffService.get(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<AcademicStaffResponse> create(@Valid @RequestBody AcademicStaffRequest request) {
    return ApiResponse.ok("Academic staff created.", academicStaffService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<AcademicStaffResponse> update(
      @PathVariable UUID id, @Valid @RequestBody AcademicStaffRequest request) {
    return ApiResponse.ok("Academic staff updated.", academicStaffService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    academicStaffService.delete(id);
    return ApiResponse.ok("Academic staff deleted.", null);
  }
}