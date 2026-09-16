package com.sitms.settings;

import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Typed application settings endpoints (SITMS-058).
 *
 * <p>Reading and updating settings is ADMIN-only. Domain services read settings internally to
 * enforce them (for example, {@code registration_open} gates internship registration). A small
 * allow-list of non-sensitive settings is readable by any authenticated user so the client can
 * render app-wide preferences such as the {@code appearance} look from the shell.
 */
@RestController
@RequestMapping("/settings")
public class AppSettingController {

  /** Keys safe to expose to any authenticated user; everything else stays ADMIN-only. */
  private static final Set<String> PUBLIC_KEYS = Set.of("appearance");

  private final AppSettingService appSettingService;

  public AppSettingController(AppSettingService appSettingService) {
    this.appSettingService = appSettingService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ApiResponse.PageData<AppSettingResponse>> list(
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(ApiResponse.PageData.from(appSettingService.list(pageable)));
  }

  @GetMapping("/public/{key}")
  public ApiResponse<AppSettingResponse> getPublic(@PathVariable String key) {
    if (!PUBLIC_KEYS.contains(key)) {
      throw new ResourceNotFoundException(
          AppSettingService.SETTING_NOT_FOUND, "Setting not found: " + key);
    }
    return ApiResponse.ok(appSettingService.findByKey(key));
  }

  @PutMapping("/{key}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<AppSettingResponse> update(
      @PathVariable String key, @Valid @RequestBody AppSettingRequest request) {
    return ApiResponse.ok(
        "Setting updated.", appSettingService.update(key, request));
  }
}