package com.sitms.common.security;

import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.internship.InternshipRepository;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.SupervisorRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves role-scoped data access helpers shared across services: the calling student profile id,
 * supervisor / academic-staff profile ids, and the set of student ids assigned to a supervisor or
 * academic staff member.
 *
 * <p>All methods resolve a profile from an {@code app_user} id and throw
 * {@link ResourceNotFoundException} (mapped to HTTP 404) when no matching profile exists, so callers
 * silently hide the existence of data they may not access.
 */
@Component
public class DataScope {

  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final AcademicStaffRepository academicStaffRepository;
  private final InternshipRepository internshipRepository;

  public DataScope(
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      AcademicStaffRepository academicStaffRepository,
      InternshipRepository internshipRepository) {
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.academicStaffRepository = academicStaffRepository;
    this.internshipRepository = internshipRepository;
  }

  /** The student profile id for the given app user id. */
  public UUID studentId(UUID appUserId) {
    return studentRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "STUDENT_PROFILE_NOT_FOUND", "No student profile for user: " + appUserId))
        .getId();
  }

  /** The supervisor profile id for the given app user id. */
  public UUID supervisorId(UUID appUserId) {
    return supervisorRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "SUPERVISOR_PROFILE_NOT_FOUND", "No supervisor profile for user: " + appUserId))
        .getId();
  }

  /** The academic staff profile id for the given app user id. */
  public UUID academicStaffId(UUID appUserId) {
    return academicStaffRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    "ACADEMIC_STAFF_PROFILE_NOT_FOUND",
                    "No academic staff profile for user: " + appUserId))
        .getId();
  }

  /** The set of student profile ids assigned (via an internship) to the given app user's supervisor. */
  public Set<UUID> assignedStudentIds(UUID appUserId) {
    return internshipRepository.findStudentIdsBySupervisorId(supervisorId(appUserId));
  }

  /** The set of student profile ids assigned (via an internship) to the given academic staff member. */
  public Set<UUID> assignedAcademicStudentIds(UUID appUserId) {
    return internshipRepository.findStudentIdsByAcademicStaffId(academicStaffId(appUserId));
  }

  /** The set of student profile ids assigned (via an internship) to a given academic staff profile id. */
  public Set<UUID> assignedAcademicStudentIdsFor(UUID academicStaffId) {
    return internshipRepository.findStudentIdsByAcademicStaffId(academicStaffId);
  }

  /** Whether a supervisor (by app user id) is assigned to the given student. */
  public boolean isAssignedToStudent(UUID appUserId, UUID studentId) {
    return internshipRepository.existsBySupervisorIdAndStudentId(
        supervisorId(appUserId), studentId);
  }

  /** Whether an academic staff member (by app user id) is assigned to the given student. */
  public boolean isAssignedToAcademicStudent(UUID appUserId, UUID studentId) {
    return internshipRepository.existsByAcademicStaffIdAndStudentId(
        academicStaffId(appUserId), studentId);
  }
}
