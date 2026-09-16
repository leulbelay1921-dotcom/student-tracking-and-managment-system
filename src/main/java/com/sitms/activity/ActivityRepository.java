package com.sitms.activity;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Activity}. */
public interface ActivityRepository
    extends JpaRepository<Activity, UUID>, JpaSpecificationExecutor<Activity> {

  Page<Activity> findByStudentId(UUID studentId, Pageable pageable);

  Page<Activity> findByInternshipId(UUID internshipId, Pageable pageable);

  Page<Activity> findByStatus(ActivityStatus status, Pageable pageable);

  List<Activity> findByStatusNot(ActivityStatus status);

  long countByStudentId(UUID studentId);

  long countByStudentIdAndStatus(UUID studentId, ActivityStatus status);

  long countByStudentIdAndStatusNot(UUID studentId, ActivityStatus status);

  long countByInternshipId(UUID internshipId);

  long countByInternshipIdAndStatus(UUID internshipId, ActivityStatus status);

  long countByInternshipIdAndStatusNot(UUID internshipId, ActivityStatus status);

  /** Total hours logged across a student's approved activities (SITMS-033, progress card). */
  @Query(
      "select coalesce(sum(a.hours), 0) from Activity a "
          + "where a.studentId = :studentId and a.status = :status")
  double sumHoursByStudentIdAndStatus(
      @Param("studentId") UUID studentId, @Param("status") ActivityStatus status);

  /** Total hours logged across a student's approved activities within one internship. */
  @Query(
      "select coalesce(sum(a.hours), 0) from Activity a "
          + "where a.internshipId = :internshipId and a.status = :status")
  double sumHoursByInternshipIdAndStatus(
      @Param("internshipId") UUID internshipId, @Param("status") ActivityStatus status);

  Page<Activity> findByStudentIdAndActivityDateBetween(
      UUID studentId, LocalDate from, LocalDate to, Pageable pageable);

  Page<Activity> findByStatusAndStudentId(ActivityStatus status, UUID studentId, Pageable pageable);

  /** Activities of students assigned to a supervisor, scoped to a given status. */
  @Query(
      "select a from Activity a "
          + "where a.status = :status and a.studentId in "
          + "(select i.studentId from Internship i where i.supervisorId = :supervisorId)")
  Page<Activity> findByStatusAndAssignedSupervisor(
      @Param("status") ActivityStatus status,
      @Param("supervisorId") UUID supervisorId,
      Pageable pageable);

  /** Activities of students assigned to an academic staff member, scoped to a status. */
  @Query(
      "select a from Activity a "
          + "where a.status = :status and a.studentId in "
          + "(select i.studentId from Internship i where i.academicStaffId = :academicStaffId)")
  Page<Activity> findByStatusAndAssignedAcademicStaff(
      @Param("status") ActivityStatus status,
      @Param("academicStaffId") UUID academicStaffId,
      Pageable pageable);
}
