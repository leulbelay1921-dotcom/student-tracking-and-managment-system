package com.sitms.student;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Payload for a student updating their own profile (PUT /students/me).
 *
 * <p>Only contact fields the student owns are writable here; official fields (name, number,
 * program, department, university, year, status) are controlled by admins via {@link StudentRequest}.
 */
public record StudentProfileUpdateRequest(
    @Email @Size(max = 255) String email,
    @Size(max = 32) String phone) {}