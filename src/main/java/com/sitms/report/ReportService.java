package com.sitms.report;

import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.activity.Activity;
import com.sitms.activity.ActivityRepository;
import com.sitms.activity.ActivityStatus;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.evaluation.Evaluation;
import com.sitms.evaluation.EvaluationRepository;
import com.sitms.evaluation.EvaluationStatus;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.internship.InternshipStatus;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.SupervisorRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Report generation over live internship, activity and evaluation data (SITMS-044, EPIC 11).
 *
 * <p>Reports are computed on demand from current data — nothing is persisted. Admins report over
 * all records; supervisors (company) over their assigned students or the evaluations they authored;
 * faculty (university supervisors) over the students assigned to them academically; students over
 * their own internships, activities and evaluations.
 */
@Service
public class ReportService {

  public static final String SUPERVISOR_PROFILE_NOT_FOUND = "SUPERVISOR_PROFILE_NOT_FOUND";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String ACADEMIC_STAFF_PROFILE_NOT_FOUND = "ACADEMIC_STAFF_PROFILE_NOT_FOUND";

  private static final String[] DIMENSIONS = {
    "technicalSkills", "communication", "teamwork", "problemSolving", "professionalism"
  };

  private final InternshipRepository internshipRepository;
  private final ActivityRepository activityRepository;
  private final EvaluationRepository evaluationRepository;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final AcademicStaffRepository academicStaffRepository;

  public ReportService(
      InternshipRepository internshipRepository,
      ActivityRepository activityRepository,
      EvaluationRepository evaluationRepository,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      AcademicStaffRepository academicStaffRepository) {
    this.internshipRepository = internshipRepository;
    this.activityRepository = activityRepository;
    this.evaluationRepository = evaluationRepository;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.academicStaffRepository = academicStaffRepository;
  }

  @Transactional(readOnly = true)
  public InternshipReport internshipReport(String role, UUID appUserId, UUID studentId) {
    List<Internship> internships =
        switch (role) {
          case "STUDENT" ->
              internshipRepository
                  .findByStudentId(resolveStudentId(appUserId), Pageable.unpaged())
                  .getContent();
          case "SUPERVISOR" ->
              internshipRepository
                  .findBySupervisorId(resolveSupervisorId(appUserId), Pageable.unpaged())
                  .getContent();
          case "FACULTY" ->
              internshipRepository
                  .findByAcademicStaffId(resolveAcademicStaffId(appUserId), Pageable.unpaged())
                  .getContent();
          default -> internshipRepository.findAll();
        };

    if (studentId != null) {
      assertStudentInScope(role, appUserId, studentId);
      internships =
          internships.stream()
              .filter(i -> studentId.equals(i.getStudentId()))
              .collect(Collectors.toList());
    }

    Map<String, Long> statusCounts = new LinkedHashMap<>();
    for (InternshipStatus status : InternshipStatus.values()) {
      statusCounts.put(status.name(), 0L);
    }
    long successful = 0;
    long decided = 0;
    for (Internship internship : internships) {
      statusCounts.merge(internship.getStatus().name(), 1L, Long::sum);
      boolean isSuccessful =
          internship.getStatus() == InternshipStatus.APPROVED
              || internship.getStatus() == InternshipStatus.COMPLETED;
      if (isSuccessful) {
        successful++;
        decided++;
      } else if (internship.getStatus() == InternshipStatus.REJECTED
          || internship.getStatus() == InternshipStatus.CANCELLED) {
        decided++;
      }
    }

    return new InternshipReport(
        ReportType.INTERNSHIP,
        OffsetDateTime.now(),
        internships.size(),
        statusCounts,
        decided == 0 ? 0.0 : round((double) successful / decided, 2),
        avgDurationDays(internships),
        byProgram(internships));
  }

  @Transactional(readOnly = true)
  public ActivityReport activityReport(String role, UUID appUserId, UUID studentId) {
    List<UUID> allowedStudentIds;
    if ("ADMIN".equals(role)) {
      allowedStudentIds = studentId == null ? null : List.of(requireStudent(studentId));
    } else if ("STUDENT".equals(role)) {
      UUID self = resolveStudentId(appUserId);
      if (studentId != null && !studentId.equals(self)) {
        throw new AccessDeniedException("Student cannot request another student's report: " + studentId);
      }
      allowedStudentIds = List.of(self);
    } else {
      Set<UUID> assigned =
          "SUPERVISOR".equals(role)
              ? internshipRepository.findStudentIdsBySupervisorId(resolveSupervisorId(appUserId))
              : internshipRepository.findStudentIdsByAcademicStaffId(resolveAcademicStaffId(appUserId));
      List<UUID> scoped = assigned.stream().distinct().collect(Collectors.toList());
      if (studentId != null) {
        if (!scoped.contains(studentId)) {
          throw new AccessDeniedException(
              "Student is not assigned to this supervisor: " + studentId);
        }
        allowedStudentIds = List.of(studentId);
      } else {
        allowedStudentIds = scoped;
      }
    }

    List<Activity> activities = activityRepository.findAll();
    if (allowedStudentIds != null) {
      activities =
          activities.stream()
              .filter(a -> allowedStudentIds.contains(a.getStudentId()))
              .collect(Collectors.toList());
    }

    Map<String, Long> statusCounts = new LinkedHashMap<>();
    for (ActivityStatus status : ActivityStatus.values()) {
      statusCounts.put(status.name(), 0L);
    }
    long approved = 0;
    long nonDraft = 0;
    for (Activity activity : activities) {
      statusCounts.merge(activity.getStatus().name(), 1L, Long::sum);
      if (activity.getStatus() != ActivityStatus.DRAFT) {
        nonDraft++;
        if (activity.getStatus() == ActivityStatus.APPROVED) {
          approved++;
        }
      }
    }
    long pendingReviews = statusCounts.getOrDefault(ActivityStatus.SUBMITTED.name(), 0L);
    boolean hasHours = activities.stream().anyMatch(a -> a.getHours() != null);
    Double totalHours =
        hasHours
            ? activities.stream()
                .map(Activity::getHours)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum()
            : null;

    return new ActivityReport(
        ReportType.ACTIVITY,
        OffsetDateTime.now(),
        studentId,
        activities.size(),
        statusCounts,
        nonDraft == 0 ? 0.0 : round((double) approved / nonDraft, 3),
        pendingReviews,
        totalHours);
  }

  @Transactional(readOnly = true)
  public EvaluationReport evaluationReport(String role, UUID appUserId, UUID studentId) {
    List<Evaluation> evaluations =
        switch (role) {
          case "STUDENT" ->
              evaluationRepository
                  .findByStudentId(resolveStudentId(appUserId), Pageable.unpaged())
                  .getContent();
          case "SUPERVISOR" ->
              evaluationRepository
                  .findBySupervisorId(resolveSupervisorId(appUserId), Pageable.unpaged())
                  .getContent();
          case "FACULTY" -> {
            Set<UUID> assigned =
                internshipRepository.findStudentIdsByAcademicStaffId(
                    resolveAcademicStaffId(appUserId));
            yield evaluationRepository.findAll().stream()
                .filter(e -> assigned.contains(e.getStudentId()))
                .collect(Collectors.toList());
          }
          default -> evaluationRepository.findAll();
        };

    if (studentId != null) {
      assertStudentInScope(role, appUserId, studentId);
      evaluations =
          evaluations.stream()
              .filter(e -> studentId.equals(e.getStudentId()))
              .collect(Collectors.toList());
    }

    List<Evaluation> submitted =
        evaluations.stream()
            .filter(e -> e.getStatus() == EvaluationStatus.SUBMITTED)
            .collect(Collectors.toList());

    Map<String, Double> dimensionAverages = new LinkedHashMap<>();
    long count = submitted.size();
    if (count == 0) {
      for (String dimension : DIMENSIONS) {
        dimensionAverages.put(dimension, null);
      }
    } else {
      for (int i = 0; i < DIMENSIONS.length; i++) {
        double sum = 0;
        for (Evaluation evaluation : submitted) {
          sum += scoreAt(evaluation, i);
        }
        dimensionAverages.put(DIMENSIONS[i], round(sum / count, 2));
      }
    }

    Double averageOverallScore =
        count == 0
            ? null
            : round(
                submitted.stream().mapToInt(Evaluation::getOverallScore).average().orElse(0),
                1);

    Map<String, Long> distribution = new LinkedHashMap<>();
    distribution.put("<60", 0L);
    distribution.put("60-69", 0L);
    distribution.put("70-79", 0L);
    distribution.put("80-89", 0L);
    distribution.put("90-100", 0L);
    for (Evaluation evaluation : submitted) {
      distribution.merge(bucket(evaluation.getOverallScore()), 1L, Long::sum);
    }

    return new EvaluationReport(
        ReportType.EVALUATION,
        OffsetDateTime.now(),
        evaluations.size(),
        count,
        evaluations.isEmpty() ? 0.0 : round((double) count / evaluations.size(), 3),
        dimensionAverages,
        averageOverallScore,
        distribution);
  }

  private static int scoreAt(Evaluation evaluation, int index) {
    return switch (index) {
      case 0 -> evaluation.getTechnicalSkills();
      case 1 -> evaluation.getCommunication();
      case 2 -> evaluation.getTeamwork();
      case 3 -> evaluation.getProblemSolving();
      default -> evaluation.getProfessionalism();
    };
  }

  private static String bucket(int overall) {
    if (overall < 60) {
      return "<60";
    }
    if (overall < 70) {
      return "60-69";
    }
    if (overall < 80) {
      return "70-79";
    }
    if (overall < 90) {
      return "80-89";
    }
    return "90-100";
  }

  private static Double avgDurationDays(Collection<Internship> internships) {
    List<Long> durations = new ArrayList<>();
    for (Internship internship : internships) {
      if (internship.getStartDate() != null
          && internship.getEndDate() != null
          && !internship.getEndDate().isBefore(internship.getStartDate())) {
        durations.add(
            ChronoUnit.DAYS.between(internship.getStartDate(), internship.getEndDate()));
      }
    }
    return durations.isEmpty()
        ? null
        : round(durations.stream().mapToLong(Long::longValue).average().orElse(0), 1);
  }

  private List<InternshipReport.ProgramStat> byProgram(List<Internship> internships) {
    if (internships.isEmpty()) {
      return List.of();
    }
    Map<UUID, String> studentProgram =
        studentRepository
            .findAllById(
                internships.stream().map(Internship::getStudentId).collect(Collectors.toSet()))
            .stream()
            .collect(
                Collectors.toMap(
                    Student::getId, s -> s.getProgram() == null ? "Unknown" : s.getProgram()));

    Map<String, long[]> byProgram = new LinkedHashMap<>();
    for (Internship internship : internships) {
      String program = studentProgram.getOrDefault(internship.getStudentId(), "Unknown");
      long[] row = byProgram.computeIfAbsent(program, k -> new long[2]);
      row[0]++;
      if (internship.getStatus() == InternshipStatus.APPROVED
          || internship.getStatus() == InternshipStatus.COMPLETED) {
        row[1]++;
      }
    }
    List<InternshipReport.ProgramStat> result = new ArrayList<>();
    byProgram.forEach(
        (program, row) ->
            result.add(
                new InternshipReport.ProgramStat(
                    program, row[0], row[1] == 0 ? 0.0 : round((double) row[1] / row[0], 2))));
    result.sort(Comparator.comparing(InternshipReport.ProgramStat::program));
    return result;
  }

  private UUID requireStudent(UUID studentId) {
    return studentRepository
        .findById(studentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    STUDENT_PROFILE_NOT_FOUND, "Student not found: " + studentId))
        .getId();
  }

  /** Ensures the requested student is within the caller's report scope. */
  private void assertStudentInScope(String role, UUID appUserId, UUID studentId) {
    requireStudent(studentId);
    switch (role) {
      case "STUDENT" -> {
        UUID self = resolveStudentId(appUserId);
        if (!self.equals(studentId)) {
          throw new AccessDeniedException(
              "Student cannot request another student's report: " + studentId);
        }
      }
      case "SUPERVISOR" -> {
        UUID supervisorId = resolveSupervisorId(appUserId);
        if (!internshipRepository.findStudentIdsBySupervisorId(supervisorId).contains(studentId)) {
          throw new AccessDeniedException(
              "Student is not assigned to this supervisor: " + studentId);
        }
      }
      case "FACULTY" -> {
        UUID academicStaffId = resolveAcademicStaffId(appUserId);
        if (!internshipRepository.findStudentIdsByAcademicStaffId(academicStaffId).contains(studentId)) {
          throw new AccessDeniedException(
              "Student is not assigned to this faculty member: " + studentId);
        }
      }
      default -> studentRepository
          .findById(studentId)
          .orElseThrow(
              () ->
                  new ResourceNotFoundException(
                      STUDENT_PROFILE_NOT_FOUND, "Student not found: " + studentId));
    }
  }

  private UUID resolveSupervisorId(UUID appUserId) {
    return supervisorRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    SUPERVISOR_PROFILE_NOT_FOUND,
                    "No supervisor profile for user: " + appUserId))
        .getId();
  }

  private UUID resolveStudentId(UUID appUserId) {
    return studentRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    STUDENT_PROFILE_NOT_FOUND,
                    "No student profile for user: " + appUserId))
        .getId();
  }

  private UUID resolveAcademicStaffId(UUID appUserId) {
    return academicStaffRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ACADEMIC_STAFF_PROFILE_NOT_FOUND,
                    "No academic staff profile for user: " + appUserId))
        .getId();
  }

  private static double round(double value, int places) {
    return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
  }
}