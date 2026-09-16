package com.sitms.organization;

import com.sitms.common.response.ApiResponse;
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
 * Organization CRUD endpoints (SITMS-012).
 *
 * <p>Reads require an authenticated user (any role); writes are ADMIN-only via
 * {@code @PreAuthorize}.
 */
@RestController
@RequestMapping("/organizations")
public class OrganizationController {

  private final OrganizationService organizationService;

  public OrganizationController(OrganizationService organizationService) {
    this.organizationService = organizationService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<OrganizationResponse>> list(
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String industry,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(
        ApiResponse.PageData.from(organizationService.list(active, industry, q, pageable)));
  }

  @GetMapping("/industries")
  public ApiResponse<ApiResponse.PageData<String>> industries(
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(ApiResponse.PageData.from(organizationService.industries(pageable)));
  }

  @GetMapping("/stats")
  public ApiResponse<OrganizationStats> stats() {
    return ApiResponse.ok(organizationService.stats());
  }

  @GetMapping("/{id}")
  public ApiResponse<OrganizationResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(organizationService.get(id));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<OrganizationResponse> create(@Valid @RequestBody OrganizationRequest request) {
    return ApiResponse.ok("Organization created.", organizationService.create(request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<OrganizationResponse> update(
      @PathVariable UUID id, @Valid @RequestBody OrganizationRequest request) {
    return ApiResponse.ok("Organization updated.", organizationService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    organizationService.delete(id);
    return ApiResponse.ok("Organization deleted.", null);
  }
}
