package com.sitms.analytics;

import com.sitms.activity.Activity;
import com.sitms.activity.ActivityRepository;
import com.sitms.activity.ActivityStatus;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.internship.InternshipStatus;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjuster;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Computes aggregate analytics over students, internships and activities (SITMS-035). */
@Service
public class AnalyticsService {

  private static final TemporalAdjuster MONDAY = temporal -> temporal.with(DayOfWeek.MONDAY);

  private final StudentRepository studentRepository;
  private final InternshipRepository internshipRepository;
  private final ActivityRepository activityRepository;
  private final DataScope dataScope;

  public AnalyticsService(
      StudentRepository studentRepository,
      InternshipRepository internshipRepository,
      ActivityRepository activityRepository,
      DataScope dataScope) {
    this.studentRepository = studentRepository;
    this.internshipRepository = internshipRepository;
    this.activityRepository = activityRepository;
    this.dataScope = dataScope;
  }

  /**
   * Computes analytics scoped to the calling principal. Admin sees all students/internships;
   * supervisors are scoped to their assigned students. When studentId is provided, the summary
   * is narrowed to that single student.
   */
  public AnalyticsSummary summary(PrincipalRole role, UUID appUserId, UUID studentId) {
    if (studentId != null) {
      return studentSummary(role, appUserId, studentId);
    }
    if (role == PrincipalRole.SUPERVISOR) {
      return supervisorSummary(appUserId);
    }
    return adminSummary();
  }

  private AnalyticsSummary studentSummary(PrincipalRole role, UUID appUserId, UUID studentId) {
    if (role == PrincipalRole.SUPERVISOR) {
      UUID supervisorId = dataScope.supervisorId(appUserId);
      if (!internshipRepository.existsBySupervisorIdAndStudentId(supervisorId, studentId)) {
        throw new org.springframework.security.access.AccessDeniedException(
            "Student is not assigned to this supervisor: " + studentId);
      }
    }
    Student student =
        studentRepository
            .findById(studentId)
            .orElseThrow(
                () ->
                    new com.sitms.common.exception.ResourceNotFoundException(
                        "STUDENT_PROFILE_NOT_FOUND", "Student not found: " + studentId));
    String program = student.getProgram() == null ? "Unknown" : student.getProgram();
    Map<String, Long> studentsByProgram = Map.of(program, 1L);

    Map<String, Long> internshipsByStatus = new LinkedHashMap<>();
    for (InternshipStatus status : InternshipStatus.values()) {
      internshipsByStatus.put(status.name(), 0L);
    }
    for (Internship internship :
        internshipRepository.findByStudentId(studentId, Pageable.unpaged())) {
      internshipsByStatus.merge(internship.getStatus().name(), 1L, Long::sum);
    }

    return new AnalyticsSummary(
        1, studentsByProgram, internshipsByStatus, activityTrends(Set.of(studentId)));
  }

  private AnalyticsSummary adminSummary() {
    List<Object[]> byProgram = studentRepository.countByProgram();
    long totalStudents = 0;
    Map<String, Long> studentsByProgram = new LinkedHashMap<>();
    for (Object[] row : byProgram) {
      String program = (String) row[0];
      long count = ((Number) row[1]).longValue();
      studentsByProgram.put(program, count);
      totalStudents += count;
    }

    Map<String, Long> internshipsByStatus = new LinkedHashMap<>();
    for (Object[] row : internshipRepository.countByStatus()) {
      internshipsByStatus.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
    }

    return new AnalyticsSummary(
        totalStudents, studentsByProgram, internshipsByStatus, activityTrends(null));
  }

  private AnalyticsSummary supervisorSummary(UUID appUserId) {
    Set<UUID> assigned = dataScope.assignedStudentIds(appUserId);

    Map<String, Long> studentsByProgram = new LinkedHashMap<>();
    for (Student student : studentRepository.findAllById(assigned)) {
      String program = student.getProgram() == null ? "Unknown" : student.getProgram();
      studentsByProgram.merge(program, 1L, Long::sum);
    }

    Map<String, Long> internshipsByStatus = new LinkedHashMap<>();
    for (InternshipStatus status : InternshipStatus.values()) {
      internshipsByStatus.put(status.name(), 0L);
    }
    for (Internship internship :
        internshipRepository.findBySupervisorId(
            dataScope.supervisorId(appUserId), Pageable.unpaged())) {
      internshipsByStatus.merge(internship.getStatus().name(), 1L, Long::sum);
    }

    return new AnalyticsSummary(
        assigned.size(), studentsByProgram, internshipsByStatus, activityTrends(assigned));
  }

  private List<AnalyticsSummary.TrendPoint> activityTrends(Set<UUID> allowedStudentIds) {
    Map<LocalDate, long[]> byWeek = new LinkedHashMap<>();
    for (Activity activity : activityRepository.findByStatusNot(ActivityStatus.DRAFT)) {
      if (allowedStudentIds != null && !allowedStudentIds.contains(activity.getStudentId())) {
        continue;
      }
      LocalDate week = activity.getActivityDate().with(MONDAY);
      long[] counts = byWeek.computeIfAbsent(week, k -> new long[2]);
      counts[0]++;
      if (activity.getStatus() == ActivityStatus.APPROVED) {
        counts[1]++;
      }
    }
    return byWeek.entrySet().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(e -> new AnalyticsSummary.TrendPoint(e.getKey(), e.getValue()[0], e.getValue()[1]))
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
