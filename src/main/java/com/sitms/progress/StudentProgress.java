package com.sitms.progress;

import java.util.UUID;

/**
 * Combined student progress derived from tasks and approved activities (SITMS-033).
 *
 * <p>{@code percent} is a 0..100 value weighting task completion and activity approval. Each
 * dimension contributes only when the student/internship has items in that dimension; when both
 * dimensions have items the two are averaged equally. {@code totalHours} aggregates the hours of
 * approved activities, and {@code evaluationScore} is the newest submitted evaluation percentage
 * (0-100), {@code null} when none exists. {@code totalAttendanceDays}/{@code presentDays} count
 * recorded attendance rows and {@code attendanceRate} is the present-day share (0-100), all
 * {@code null} until the first record exists.
 */
public record StudentProgress(
    UUID studentId,
    UUID internshipId,
    long totalTasks,
    long completedTasks,
    long attemptedActivities,
    long approvedActivities,
    double percent,
    double totalHours,
    Integer evaluationScore,
    Long totalAttendanceDays,
    Long presentDays,
    Double attendanceRate) {}
