package com.sitms.attendance;

import java.time.LocalDate;

/** Payload for a student self check-out ({@code date} defaults to today). */
public record AttendanceCheckOutRequest(LocalDate date) {}