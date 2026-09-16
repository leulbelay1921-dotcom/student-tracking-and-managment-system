package com.sitms.ticket;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Student support ticket endpoints (SITMS-073).
 *
 * <p>Any authenticated user may raise a ticket (scoped server-side to the calling student or an
 * assigned supervisor/faculty member). Updating a ticket's status/assignment is limited to
 * SUPERVISOR, FACULTY and ADMIN.
 */
@RestController
@RequestMapping("/tickets")
public class TicketController {

  private final TicketService ticketService;

  public TicketController(TicketService ticketService) {
    this.ticketService = ticketService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<TicketResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID internshipId,
      @RequestParam(required = false) TicketStatus status,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            ticketService.list(role, appUserId, studentId, internshipId, status, pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<TicketResponse> get(JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(ticketService.get(id, role, appUserId));
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('STUDENT','SUPERVISOR','FACULTY','ADMIN')")
  public ApiResponse<TicketResponse> create(
      JwtAuthenticationToken token, @Valid @RequestBody TicketRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId =
        role == PrincipalRole.ADMIN ? null : PrincipalResolver.subject(token);
    return ApiResponse.ok("Ticket created.", ticketService.create(role, appUserId, request));
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR','FACULTY')")
  public ApiResponse<TicketResponse> update(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody TicketUpdateRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role == PrincipalRole.ADMIN ? null : PrincipalResolver.subject(token);
    return ApiResponse.ok("Ticket updated.", ticketService.update(id, role, appUserId, request));
  }
}