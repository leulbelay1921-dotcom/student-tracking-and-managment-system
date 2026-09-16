package com.sitms.internship;

import java.util.UUID;

/**
 * Payload for assigning (or reassigning) the company supervisor and academic supervisor of an
 * {@link Internship}. Either value may be null to leave that slot unchanged.
 */
public record InternshipSupervisorAssignmentRequest(UUID supervisorId, UUID academicStaffId) {}