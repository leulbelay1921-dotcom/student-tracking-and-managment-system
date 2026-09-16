package com.sitms.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalTime;

/**
 * Payload for reviewing a pending attendance issue. On approval the reviewer may carry the
 * correction over to the attendance row via the optional {@code corrected*} fields (SITMS-070).
 */
public record AttendanceIssueReviewRequest(
    @NotNull AttendanceRequestStatus decision,
    @Size(max = 2000, message = "Feedback must be at most 2000 characters.") String feedback,
    AttendanceStatus correctedStatus,
    LocalTime correctedCheckIn,
    LocalTime correctedCheckOut,
    @Size(max = 500, message = "Corrected note must be at most 500 characters.")
        String correctedNote) {}