package com.sitms.audit;

import com.sitms.common.response.ApiResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin-only audit trail endpoints (SITMS-056, EPIC 13). */
@RestController
@RequestMapping("/audit-logs")
public class AuditLogController {

  private final AuditLogService auditLogService;

  public AuditLogController(AuditLogService auditLogService) {
    this.auditLogService = auditLogService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<ApiResponse.PageData<AuditLogResponse>> list(
      @RequestParam(required = false) AuditAction action,
      @PageableDefault(size = 20) Pageable pageable) {
    return ApiResponse.ok(
        ApiResponse.PageData.from(auditLogService.list(action, pageable)));
  }
}