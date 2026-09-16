package com.sitms.activity;

import com.sitms.attachment.AttachmentInfo;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** API view of an {@link Activity}, optionally including its review feedback and attachments. */
public record ActivityResponse(
    UUID id,
    UUID studentId,
    UUID internshipId,
    LocalDate activityDate,
    ActivityType type,
    String title,
    String description,
    String skills,
    Double hours,
    List<AttachmentInfo> attachments,
    ActivityStatus status,
    ReviewInfo review,
    ReviewInfo academicReview,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  /** Lightweight view of an {@link ActivityReview} surfaced to the owner/reviewer. */
  public record ReviewInfo(UUID reviewerId, ReviewDecision decision, String feedback) {

    static ReviewInfo from(ActivityReview review) {
      return new ReviewInfo(
          review.getReviewerId(), review.getDecision(), review.getFeedback());
    }
  }

  static ActivityResponse from(Activity activity) {
    return from(activity, null, List.of());
  }

  static ActivityResponse from(Activity activity, ActivityReview review) {
    return from(activity, review, List.of());
  }

  static ActivityResponse from(
      Activity activity, ActivityReview review, List<AttachmentInfo> attachments) {
    return new ActivityResponse(
        activity.getId(),
        activity.getStudentId(),
        activity.getInternshipId(),
        activity.getActivityDate(),
        activity.getType(),
        activity.getTitle(),
        activity.getDescription(),
        activity.getSkills(),
        activity.getHours(),
        attachments,
        activity.getStatus(),
        review == null ? null : ReviewInfo.from(review),
        ActivityResponse.academicReviewOf(activity),
        activity.getCreatedAt(),
        activity.getUpdatedAt());
  }

  /** Builds the academic stage verdict from the activity's own columns, when present. */
  static ReviewInfo academicReviewOf(Activity activity) {
    if (activity.getAcademicReviewerId() == null) {
      return null;
    }
    boolean approved = activity.getStatus() == ActivityStatus.APPROVED;
    return new ReviewInfo(
        activity.getAcademicReviewerId(),
        approved ? ReviewDecision.APPROVED : ReviewDecision.REJECTED,
        activity.getAcademicFeedback());
  }
}