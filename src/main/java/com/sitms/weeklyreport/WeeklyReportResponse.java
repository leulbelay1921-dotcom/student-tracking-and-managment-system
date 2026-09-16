package com.sitms.weeklyreport;

import com.sitms.attachment.AttachmentInfo;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Read model of a {@link WeeklyReport} plus its attachments and the owning student's name. */
public record WeeklyReportResponse(
    UUID id,
    UUID studentId,
    String studentName,
    UUID internshipId,
    Integer weekNumber,
    String title,
    String content,
    String activitiesCompleted,
    String problemsEncountered,
    String skillsLearned,
    String nextWeekPlan,
    WeeklyReportStatus status,
    UUID companyReviewerId,
    String companyReviewNote,
    UUID academicReviewerId,
    String academicReviewNote,
    List<AttachmentInfo> attachments,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static WeeklyReportResponse from(
      WeeklyReport report, String studentName, List<AttachmentInfo> attachments) {
    return new WeeklyReportResponse(
        report.getId(),
        report.getStudentId(),
        studentName,
        report.getInternshipId(),
        report.getWeekNumber(),
        report.getTitle(),
        report.getContent(),
        report.getActivitiesCompleted(),
        report.getProblemsEncountered(),
        report.getSkillsLearned(),
        report.getNextWeekPlan(),
        report.getStatus(),
        report.getCompanyReviewerId(),
        report.getCompanyReviewNote(),
        report.getAcademicReviewerId(),
        report.getAcademicReviewNote(),
        attachments,
        report.getCreatedAt(),
        report.getUpdatedAt());
  }

  static WeeklyReportResponse from(WeeklyReport report, String studentName) {
    return from(report, studentName, List.of());
  }
}