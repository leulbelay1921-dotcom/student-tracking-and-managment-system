package com.sitms.attendance;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Payload for marking a day absent ({@code date} defaults to today when omitted). */
public record AttendanceAbsentRequest(
    LocalDate date, @Size(max = 500) String note) {}