package com.sitms.attendance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/** Payload for reviewing a leave request or attendance issue. */
public record AttendanceRequestReviewRequest(
    @NotNull AttendanceRequestStatus decision, @Size(max = 2000) String feedback) {}