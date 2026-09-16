package com.sitms.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sitms.activity.ActivityRepository;
import com.sitms.activity.ActivityStatus;
import com.sitms.attendance.AttendanceRepository;
import com.sitms.attendance.AttendanceStatus;
import com.sitms.evaluation.Evaluation;
import com.sitms.evaluation.EvaluationRepository;
import com.sitms.evaluation.EvaluationStatus;
import com.sitms.task.TaskRepository;
import com.sitms.task.TaskStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link ProgressService} (SITMS-058): the two-dimension completion formula is
 * pinned in isolation — no items → 0, a single dimension dominates when the other is empty, and
 * both dimensions are averaged (rounded to 0.1) when both have items. Attendance adds its own
 * present-day total and rate, which stay null until the first record.
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

  private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID INTERNSHIP_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

  @Mock private TaskRepository taskRepository;
  @Mock private ActivityRepository activityRepository;
  @Mock private EvaluationRepository evaluationRepository;
  @Mock private AttendanceRepository attendanceRepository;
  @InjectMocks private ProgressService progressService;

  @Test
  void noScopeAtAllReturnsZeroProgressWithoutHittingRepositories() {
    StudentProgress progress = progressService.compute(null, null);

    assertThat(progress.percent()).isZero();
    assertThat(progress.totalTasks()).isZero();
    assertThat(progress.completedTasks()).isZero();
    assertThat(progress.attemptedActivities()).isZero();
    assertThat(progress.approvedActivities()).isZero();
    assertThat(progress.totalAttendanceDays()).isNull();
    assertThat(progress.presentDays()).isNull();
    assertThat(progress.attendanceRate()).isNull();
    verifyNoInteractions(taskRepository, activityRepository, attendanceRepository);
  }

  @Test
  void studentWithNoItemsHasZeroPercent() {
    when(taskRepository.countByStudentId(STUDENT_ID)).thenReturn(0L);
    when(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED)).thenReturn(0L);
    when(activityRepository.countByStudentIdAndStatusNot(STUDENT_ID, ActivityStatus.DRAFT))
        .thenReturn(0L);
    when(activityRepository.countByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(0L);
    when(evaluationRepository.findFirstByStudentIdOrderByCreatedAtDesc(STUDENT_ID))
        .thenReturn(Optional.empty());

    StudentProgress progress = progressService.compute(null, STUDENT_ID);

    assertThat(progress.percent()).isZero();
    assertThat(progress.studentId()).isEqualTo(STUDENT_ID);
    assertThat(progress.totalHours()).isZero();
    assertThat(progress.evaluationScore()).isNull();
    assertThat(progress.totalAttendanceDays()).isNull();
    assertThat(progress.presentDays()).isNull();
    assertThat(progress.attendanceRate()).isNull();
  }

  @Test
  void tasksOnlyDimensionDominatesWhenNoActivitiesAttempted() {
    when(taskRepository.countByStudentId(STUDENT_ID)).thenReturn(4L);
    when(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED)).thenReturn(3L);
    when(activityRepository.countByStudentIdAndStatusNot(STUDENT_ID, ActivityStatus.DRAFT))
        .thenReturn(0L);
    when(activityRepository.countByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(0L);
    when(evaluationRepository.findFirstByStudentIdOrderByCreatedAtDesc(STUDENT_ID))
        .thenReturn(Optional.empty());

    StudentProgress progress = progressService.compute(null, STUDENT_ID);

    assertThat(progress.percent()).isEqualTo(75.0);
    assertThat(progress.totalTasks()).isEqualTo(4);
    assertThat(progress.completedTasks()).isEqualTo(3);
    assertThat(progress.totalHours()).isZero();
  }

  @Test
  void activitiesOnlyDimensionDominatesWhenNoTasksExist() {
    when(taskRepository.countByStudentId(STUDENT_ID)).thenReturn(0L);
    when(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED)).thenReturn(0L);
    when(activityRepository.countByStudentIdAndStatusNot(STUDENT_ID, ActivityStatus.DRAFT))
        .thenReturn(4L);
    when(activityRepository.countByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(2L);
    when(evaluationRepository.findFirstByStudentIdOrderByCreatedAtDesc(STUDENT_ID))
        .thenReturn(Optional.empty());

    StudentProgress progress = progressService.compute(null, STUDENT_ID);

    assertThat(progress.percent()).isEqualTo(50.0);
    assertThat(progress.approvedActivities()).isEqualTo(2);
    assertThat(progress.attemptedActivities()).isEqualTo(4);
  }

  @Test
  void bothDimensionsAreAveragedAndRoundedToOneDecimal() {
    when(taskRepository.countByStudentId(STUDENT_ID)).thenReturn(3L);
    when(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED)).thenReturn(1L);
    when(activityRepository.countByStudentIdAndStatusNot(STUDENT_ID, ActivityStatus.DRAFT))
        .thenReturn(2L);
    when(activityRepository.countByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(1L);
    when(evaluationRepository.findFirstByStudentIdOrderByCreatedAtDesc(STUDENT_ID))
        .thenReturn(Optional.empty());

    StudentProgress progress = progressService.compute(null, STUDENT_ID);

    double taskPercent = (1.0 * 100.0) / 3; // 33.3...
    double activityPercent = 50.0;
    double expected = Math.round(((taskPercent + activityPercent) / 2.0) * 10.0) / 10.0;
    assertThat(progress.percent()).isEqualTo(expected).as("average of 33.3 and 50 rounds to");
    assertThat(progress.percent()).isEqualTo(41.7);
  }

  @Test
  void internshipScopeRoutesCountersByInternship() {
    when(taskRepository.countByInternshipId(INTERNSHIP_ID)).thenReturn(2L);
    when(taskRepository.countByInternshipIdAndStatus(INTERNSHIP_ID, TaskStatus.COMPLETED))
        .thenReturn(2L);
    when(activityRepository.countByInternshipIdAndStatusNot(INTERNSHIP_ID, ActivityStatus.DRAFT))
        .thenReturn(0L);
    when(activityRepository.countByInternshipIdAndStatus(INTERNSHIP_ID, ActivityStatus.APPROVED))
        .thenReturn(0L);
    when(evaluationRepository.findByStudentIdAndInternshipId(null, INTERNSHIP_ID))
        .thenReturn(List.of());

    StudentProgress progress = progressService.compute(INTERNSHIP_ID, null);

    assertThat(progress.percent()).isEqualTo(100.0);
    assertThat(progress.internshipId()).isEqualTo(INTERNSHIP_ID);
    verify(taskRepository).countByInternshipIdAndStatus(INTERNSHIP_ID, TaskStatus.COMPLETED);
    verify(activityRepository).countByInternshipIdAndStatusNot(INTERNSHIP_ID, ActivityStatus.DRAFT);
  }

  @Test
  void approvedHoursAndSubmittedEvaluationFeedTheProgressCard() {
    when(taskRepository.countByStudentId(STUDENT_ID)).thenReturn(2L);
    when(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED)).thenReturn(2L);
    when(activityRepository.countByStudentIdAndStatusNot(STUDENT_ID, ActivityStatus.DRAFT))
        .thenReturn(3L);
    when(activityRepository.countByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(2L);
    when(activityRepository.sumHoursByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(23.5);
    Evaluation submitted =
        Evaluation.builder()
            .studentId(STUDENT_ID)
            .internshipId(INTERNSHIP_ID)
            .supervisorId(UUID.fromString("33333333-3333-3333-3333-333333333333"))
            .status(EvaluationStatus.SUBMITTED)
            .technicalSkills(5)
            .communication(4)
            .teamwork(4)
            .problemSolving(4)
            .professionalism(4)
            .build();
    when(evaluationRepository.findFirstByStudentIdOrderByCreatedAtDesc(STUDENT_ID))
        .thenReturn(Optional.of(submitted));

    StudentProgress progress = progressService.compute(null, STUDENT_ID);

    assertThat(progress.totalHours()).isEqualTo(23.5);
    assertThat(progress.evaluationScore()).isEqualTo(84);
  }

  @Test
  void recordedAttendanceFeedsTotalPresentDaysAndRate() {
    when(taskRepository.countByStudentId(STUDENT_ID)).thenReturn(0L);
    when(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED)).thenReturn(0L);
    when(activityRepository.countByStudentIdAndStatusNot(STUDENT_ID, ActivityStatus.DRAFT))
        .thenReturn(0L);
    when(activityRepository.countByStudentIdAndStatus(STUDENT_ID, ActivityStatus.APPROVED))
        .thenReturn(0L);
    when(evaluationRepository.findFirstByStudentIdOrderByCreatedAtDesc(STUDENT_ID))
        .thenReturn(Optional.empty());
    when(attendanceRepository.countByStudentIdAndStatus(STUDENT_ID, AttendanceStatus.PRESENT))
        .thenReturn(18L);
    when(attendanceRepository.countByStudentIdAndStatus(STUDENT_ID, AttendanceStatus.ABSENT))
        .thenReturn(2L);

    StudentProgress progress = progressService.compute(null, STUDENT_ID);

    assertThat(progress.totalAttendanceDays()).isEqualTo(20);
    assertThat(progress.presentDays()).isEqualTo(18);
    assertThat(progress.attendanceRate()).isEqualTo(90.0);
  }
}