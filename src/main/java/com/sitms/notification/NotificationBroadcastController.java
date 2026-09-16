package com.sitms.notification;

import com.sitms.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Notification broadcasting (POST /notifications/broadcast).
 *
 * <p>Any authenticated user may send an {@code ADMIN_BROADCAST} notification to a recipient group
 * (all students, company supervisors, university supervisors, a specific student, or a specific
 * organization). The {@code /notifications/**} namespace is authenticated-only via
 * {@link com.sitms.common.security.SecurityConfig}; the {@code @PreAuthorize} is defense in depth.
 */
@RestController
@RequestMapping("/notifications/broadcast")
public class NotificationBroadcastController {

  private final NotificationBroadcastService notificationBroadcastService;

  public NotificationBroadcastController(
      NotificationBroadcastService notificationBroadcastService) {
    this.notificationBroadcastService = notificationBroadcastService;
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ApiResponse<BroadcastSummary> send(@Valid @RequestBody AdminBroadcastRequest request) {
    BroadcastSummary summary = notificationBroadcastService.broadcast(request);
    String message =
        summary.recipients() == 1
            ? "Notification sent to 1 recipient."
            : "Notification sent to " + summary.recipients() + " recipients.";
    return ApiResponse.ok(message, summary);
  }
}