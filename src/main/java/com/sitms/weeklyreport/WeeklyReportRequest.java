package com.sitms.weeklyreport;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/** Payload for creating or editing a weekly report, including optional attached documents. */
public record WeeklyReportRequest(
    @NotNull @Min(value = 1L, message = "Week number must be at least 1.")
        @Max(value = 260L, message = "Week number is unreasonably high.")
        Integer weekNumber,
    @Size(max = 128, message = "Title must be at most 128 characters.") String title,
    @Size(max = 10000, message = "Content must be at most 10000 characters.") String content,
    @Size(max = 10000, message = "Activities completed must be at most 10000 characters.")
        String activitiesCompleted,
    @Size(max = 10000, message = "Problems encountered must be at most 10000 characters.")
        String problemsEncountered,
    @Size(max = 10000, message = "Skills learned must be at most 10000 characters.")
        String skillsLearned,
    @Size(max = 10000, message = "Next week's plan must be at most 10000 characters.")
        String nextWeekPlan,
    UUID internshipId,
    List<UUID> attachmentDocumentIds) {}