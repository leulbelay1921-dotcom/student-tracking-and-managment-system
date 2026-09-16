package com.sitms.notification;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.response.ApiResponse;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification endpoints (SITMS-051). Every authenticated role (ADMIN/STUDENT/SUPERVISOR/FACULTY) may read
 * and mark read only its own notifications; scoping is enforced in {@link NotificationService}.
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

  public static final String USER_NOT_FOUND = "USER_NOT_FOUND";

  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<ApiResponse.PageData<NotificationResponse>> list(
      Authentication authentication, @PageableDefault(size = 20) Pageable pageable) {
    Principal principal = principal(authentication);
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            notificationService.list(principal.role(), principal.name(), pageable)));
  }

  @GetMapping("/unread-count")
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<Long> unreadCount(Authentication authentication) {
    Principal principal = principal(authentication);
    return ApiResponse.ok(notificationService.unreadCount(principal.role(), principal.name()));
  }

  @PatchMapping("/{id}/read")
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<NotificationResponse> markRead(
      Authentication authentication, @PathVariable UUID id) {
    Principal principal = principal(authentication);
    return ApiResponse.ok(
        "Notification marked as read.",
        notificationService.markRead(id, principal.role(), principal.name()));
  }

  @PatchMapping("/read-all")
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<Long> markAllRead(Authentication authentication) {
    Principal principal = principal(authentication);
    long marked = notificationService.markAllRead(principal.role(), principal.name());
    return ApiResponse.ok(marked + " notification(s) marked as read.", marked);
  }

  private static Principal principal(Authentication authentication) {
    String role =
        authentication.getAuthorities().stream()
            .map(Object::toString)
            .map(a -> a.replace("ROLE_", ""))
            .filter(a -> a.equals("ADMIN") || a.equals("STUDENT") || a.equals("SUPERVISOR") || a.equals("FACULTY"))
            .findFirst()
            .orElseThrow(
                () ->
                    new BusinessException(
                        USER_NOT_FOUND, "Unrecognized principal role."));
    return new Principal(role, authentication.getName());
  }

  private record Principal(String role, String name) {}
}