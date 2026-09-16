package com.sitms.progress;

import com.sitms.activity.ActivityRepository;
import com.sitms.activity.ActivityStatus;
import com.sitms.attendance.AttendanceRepository;
import com.sitms.attendance.AttendanceStatus;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.evaluation.Evaluation;
import com.sitms.evaluation.EvaluationRepository;
import com.sitms.evaluation.EvaluationStatus;
import com.sitms.internship.InternshipRepository;
import com.sitms.task.TaskRepository;
import com.sitms.task.TaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Computes a student's overall progress from their tasks and approved activities (SITMS-033).
 *
 * <p>Two independent dimensions are combined:
 *
 * <ul>
 *   <li>Task completion: {@code completedTasks / totalTasks}.
 *   <li>Activity approval: {@code approvedActivities / attemptedActivities}, where an activity is
 *       "attempted" once it has left the DRAFT state (submitted, approved or rejected).
 * </ul>
 *
 * <p>Each dimension only contributes when the student/internship has items in it. When one dimension
 * has no items the other dominates; when both have items the two percentages are averaged equally.
 * With no items at all the percent is 0.0. Hour totals come from approved activities, the
 * academic-standing score from the student's newest submitted evaluation, and recorded attendance
 * contributes its present-day total and rate (null until the first record).
 */
@Service
public class ProgressService {

  private final TaskRepository taskRepository;
  private final ActivityRepository activityRepository;
  private final EvaluationRepository evaluationRepository;
  private final AttendanceRepository attendanceRepository;
  private final InternshipRepository internshipRepository;
  private final DataScope dataScope;

  public ProgressService(
      TaskRepository taskRepository,
      ActivityRepository activityRepository,
      EvaluationRepository evaluationRepository,
      AttendanceRepository attendanceRepository,
      InternshipRepository internshipRepository,
      DataScope dataScope) {
    this.taskRepository = taskRepository;
    this.activityRepository = activityRepository;
    this.evaluationRepository = evaluationRepository;
    this.attendanceRepository = attendanceRepository;
    this.internshipRepository = internshipRepository;
    this.dataScope = dataScope;
  }

  /**
   * Computes progress for a student, verifying the principal may view it.
   *
   * <p>ADMIN sees any target; SUPERVISOR only their assigned students; STUDENT only their own
   * profile (a supplied studentId for another student yields {@link AccessDeniedException}).
   */
  public StudentProgress computeFor(
      PrincipalRole role,
      UUID appUserId,
      UUID internshipId,
      UUID studentId,
      UUID supervisorId,
      UUID academicStaffId) {
    if (role == PrincipalRole.STUDENT) {
      UUID own = dataScope.studentId(appUserId);
      if (studentId != null && !studentId.equals(own)) {
        throw new AccessDeniedException("Progress is only available for your own profile.");
      }
      return compute(internshipId, own);
    }
    if (role == PrincipalRole.SUPERVISOR && studentId != null) {
      UUID targetSupervisorId =
          supervisorId != null ? supervisorId : dataScope.supervisorId(appUserId);
      if (!internshipRepository.existsBySupervisorIdAndStudentId(
          targetSupervisorId, studentId)) {
        throw new AccessDeniedException("Student is not assigned to this supervisor: " + studentId);
      }
    }
    if (role == PrincipalRole.FACULTY && studentId != null) {
      if (academicStaffId != null) {
        if (!internshipRepository.existsByAcademicStaffIdAndStudentId(
            academicStaffId, studentId)) {
          throw new AccessDeniedException(
              "Student is not assigned to this academic staff member: " + studentId);
        }
      } else if (!dataScope.isAssignedToAcademicStudent(appUserId, studentId)) {
        throw new AccessDeniedException(
            "Student is not assigned to this academic staff member: " + studentId);
      }
    }
    return compute(internshipId, studentId);
  }

  /** Computes progress for a student, optionally scoped to one internship. */
  public StudentProgress compute(UUID internshipId, UUID studentId) {
    if (internshipId == null && studentId == null) {
      return new StudentProgress(
          null, null, 0, 0, 0, 0, 0.0, 0.0, null, null, null, null);
    }

    boolean scoped = internshipId != null;
    long totalTasks;
    long completedTasks;
    long attemptedActivities;
    long approvedActivities;
    double totalHours;
    Integer evaluationScore;
    Long presentDays;
    Long absentDays;
    if (scoped) {
      totalTasks = taskRepository.countByInternshipId(internshipId);
      completedTasks =
          taskRepository.countByInternshipIdAndStatus(internshipId, TaskStatus.COMPLETED);
      attemptedActivities =
          activityRepository.countByInternshipIdAndStatusNot(internshipId, ActivityStatus.DRAFT);
      approvedActivities =
          activityRepository.countByInternshipIdAndStatus(internshipId, ActivityStatus.APPROVED);
      totalHours =
          activityRepository.sumHoursByInternshipIdAndStatus(
              internshipId, ActivityStatus.APPROVED);
      evaluationScore =
          submittedScore(
              evaluationRepository.findByStudentIdAndInternshipId(studentId, internshipId));
      presentDays =
          attendanceRepository.countByInternshipIdAndStatus(internshipId, AttendanceStatus.PRESENT);
      absentDays =
          attendanceRepository.countByInternshipIdAndStatus(internshipId, AttendanceStatus.ABSENT);
    } else {
      totalTasks = taskRepository.countByStudentId(studentId);
      completedTasks =
          taskRepository.countByStudentIdAndStatus(studentId, TaskStatus.COMPLETED);
      attemptedActivities =
          activityRepository.countByStudentIdAndStatusNot(studentId, ActivityStatus.DRAFT);
      approvedActivities =
          activityRepository.countByStudentIdAndStatus(studentId, ActivityStatus.APPROVED);
      totalHours =
          activityRepository.sumHoursByStudentIdAndStatus(studentId, ActivityStatus.APPROVED);
      evaluationScore =
          submittedScore(
              evaluationRepository
                  .findFirstByStudentIdOrderByCreatedAtDesc(studentId)
                  .map(List::of)
                  .orElseGet(List::of));
      presentDays =
          attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.PRESENT);
      absentDays =
          attendanceRepository.countByStudentIdAndStatus(studentId, AttendanceStatus.ABSENT);
    }

    double taskPercent = totalTasks == 0 ? -1.0 : (completedTasks * 100.0) / totalTasks;
    double activityPercent =
        attemptedActivities == 0 ? -1.0 : (approvedActivities * 100.0) / attemptedActivities;

    double percent;
    if (taskPercent >= 0 && activityPercent >= 0) {
      percent = (taskPercent + activityPercent) / 2.0;
    } else if (taskPercent >= 0) {
      percent = taskPercent;
    } else if (activityPercent >= 0) {
      percent = activityPercent;
    } else {
      percent = 0.0;
    }

    double rounded = Math.round(percent * 10.0) / 10.0;
    Long totalAttendance = attendance(totalDays(presentDays, absentDays));
    return new StudentProgress(
        studentId,
        internshipId,
        totalTasks,
        completedTasks,
        attemptedActivities,
        approvedActivities,
        rounded,
        totalHours,
        evaluationScore,
        totalAttendance,
        totalAttendance == null ? null : presentDays,
        attendanceRate(presentDays, absentDays));
  }

  private static long totalDays(Long presentDays, Long absentDays) {
    return (presentDays == null ? 0L : presentDays) + (absentDays == null ? 0L : absentDays);
  }

  private static Long attendance(long totalDays) {
    return totalDays == 0 ? null : totalDays;
  }

  private static Double attendanceRate(Long presentDays, Long absentDays) {
    long total = totalDays(presentDays, absentDays);
    if (total == 0 || presentDays == null) {
      return null;
    }
    return Math.round((presentDays * 100.0) / total * 10.0) / 10.0;
  }

  /** Overall score (0-100) of the newest submitted evaluation, or {@code null} if none. */
  private Integer submittedScore(List<Evaluation> evaluations) {
    return evaluations.stream()
        .filter(e -> e != null && e.getStatus() == EvaluationStatus.SUBMITTED)
        .map(Evaluation::getOverallScore)
        .max(Integer::compareTo)
        .orElse(null);
  }
}
