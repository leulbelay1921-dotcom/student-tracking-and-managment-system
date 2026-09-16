package com.sitms.activity;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.config.JpaAuditingConfig;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository integration test for {@link Activity} and {@link ActivityReview} against a real
 * Postgres (SITMS-027): persist/find, audit columns, reference + status queries, and review of an
 * activity.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class ActivityRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_STUDENT_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID INTERNSHIP_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

  @Autowired private ActivityRepository activityRepository;
  @Autowired private ActivityReviewRepository reviewRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_ID, "student1");
    seedAppUser(OTHER_STUDENT_ID, "student2");
    seedAppUser(SUPERVISOR_ID, "supervisor1");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_ID,
        ORGANIZATION_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        AUDITOR_ID);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void seedAppUser(UUID id, String username) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, 'ADMIN', TRUE, ?)",
        id,
        username,
        username + "@example.com",
        username,
        AUDITOR_ID);
  }

  @Test
  void persistsAndRetrievesActivityWithDefaults() {
    setAuditor(AUDITOR_ID);
    Activity saved =
        activityRepository.save(
            Activity.builder()
                .studentId(STUDENT_ID)
                .internshipId(INTERNSHIP_ID)
                .activityDate(LocalDate.of(2026, 9, 1))
                .type(ActivityType.DAILY)
                .description("Worked on the reporting module")
                .hours(3.5)
                .build());

    Activity found = activityRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStudentId()).isEqualTo(STUDENT_ID);
    assertThat(found.getInternshipId()).isEqualTo(INTERNSHIP_ID);
    assertThat(found.getActivityDate()).isEqualTo(LocalDate.of(2026, 9, 1));
    assertThat(found.getType()).isEqualTo(ActivityType.DAILY);
    assertThat(found.getDescription()).isEqualTo("Worked on the reporting module");
    assertThat(found.getHours()).isEqualTo(3.5);
    assertThat(found.getStatus()).isEqualTo(ActivityStatus.DRAFT);
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Activity saved = activityRepository.save(newActivity(STUDENT_ID, "Desc"));
    activityRepository.flush();

    Activity found = activityRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByStudentPaged() {
    setAuditor(AUDITOR_ID);
    activityRepository.save(newActivity(STUDENT_ID, "A"));
    activityRepository.save(newActivity(STUDENT_ID, "B"));
    activityRepository.save(newActivity(OTHER_STUDENT_ID, "C"));

    Page<Activity> page = activityRepository.findByStudentId(STUDENT_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findsByInternshipAndStatusPaged() {
    setAuditor(AUDITOR_ID);
    activityRepository.save(newActivity(STUDENT_ID, "A"));
    Activity submitted =
        activityRepository.save(
            Activity.builder()
                .studentId(OTHER_STUDENT_ID)
                .activityDate(LocalDate.of(2026, 9, 2))
                .description("Submitted")
                .status(ActivityStatus.SUBMITTED)
                .build());

    Page<Activity> byInternship =
        activityRepository.findByInternshipId(INTERNSHIP_ID, PageRequest.of(0, 10));
    assertThat(byInternship.getTotalElements()).isEqualTo(1);

    Page<Activity> submittedPage =
        activityRepository.findByStatus(ActivityStatus.SUBMITTED, PageRequest.of(0, 10));
    assertThat(submittedPage.getTotalElements()).isEqualTo(1);
  }

  @Test
  void findsByStudentAndDateRange() {
    setAuditor(AUDITOR_ID);
    activityRepository.save(
        Activity.builder()
            .studentId(STUDENT_ID)
            .activityDate(LocalDate.of(2026, 9, 1))
            .description("Mon")
            .build());
    activityRepository.save(
        Activity.builder()
            .studentId(STUDENT_ID)
            .activityDate(LocalDate.of(2026, 9, 10))
            .description("Later")
            .build());

    Page<Activity> week =
        activityRepository.findByStudentIdAndActivityDateBetween(
            STUDENT_ID,
            LocalDate.of(2026, 9, 1),
            LocalDate.of(2026, 9, 7),
            PageRequest.of(0, 10));
    assertThat(week.getTotalElements()).isEqualTo(1);
    assertThat(week.getContent().get(0).getDescription()).isEqualTo("Mon");
  }

  @Test
  void reviewsAnActivityAndTransitionsStatus() {
    setAuditor(AUDITOR_ID);
    Activity saved = activityRepository.saveAndFlush(newActivity(STUDENT_ID, "Desc"));

    ActivityReview review =
        reviewRepository.saveAndFlush(
            ActivityReview.builder()
                .activityId(saved.getId())
                .reviewerId(SUPERVISOR_ID)
                .decision(ReviewDecision.APPROVED)
                .feedback("Good work.")
                .build());

    saved.changeStatus(ActivityStatus.APPROVED);
    activityRepository.saveAndFlush(saved);

    ActivityReview found = reviewRepository.findByActivityId(saved.getId()).orElseThrow();
    assertThat(found.getId()).isEqualTo(review.getId());
    assertThat(found.getDecision()).isEqualTo(ReviewDecision.APPROVED);
    assertThat(found.getFeedback()).isEqualTo("Good work.");

    assertThat(
            reviewRepository.countByReviewerIdAndDecision(
                SUPERVISOR_ID, ReviewDecision.APPROVED))
        .isEqualTo(1);
    assertThat(activityRepository.findById(saved.getId()).orElseThrow().getStatus())
        .isEqualTo(ActivityStatus.APPROVED);
  }

  @Test
  void findsReviewsByActivityIdsAndByReviewer() {
    setAuditor(AUDITOR_ID);
    Activity first = activityRepository.saveAndFlush(newActivity(STUDENT_ID, "First"));
    Activity second = activityRepository.saveAndFlush(newActivity(STUDENT_ID, "Second"));
    Activity third = activityRepository.saveAndFlush(newActivity(STUDENT_ID, "Unreviewed"));
    reviewRepository.saveAndFlush(
        ActivityReview.builder()
            .activityId(first.getId())
            .reviewerId(SUPERVISOR_ID)
            .decision(ReviewDecision.APPROVED)
            .feedback("Good.")
            .build());
    reviewRepository.saveAndFlush(
        ActivityReview.builder()
            .activityId(second.getId())
            .reviewerId(SUPERVISOR_ID)
            .decision(ReviewDecision.REJECTED)
            .feedback("Revise.")
            .build());

    assertThat(reviewRepository.findByActivityIdIn(List.of(first.getId(), second.getId())))
        .hasSize(2);
    assertThat(reviewRepository.findByActivityIdIn(List.of(third.getId()))).isEmpty();

    Page<ActivityReview> byReviewer =
        reviewRepository.findByReviewerId(SUPERVISOR_ID, PageRequest.of(0, 10));
    assertThat(byReviewer.getTotalElements()).isEqualTo(2);
    assertThat(
        byReviewer.getContent().stream()
            .map(ActivityReview::getDecision)
            .toList())
        .containsExactlyInAnyOrder(ReviewDecision.APPROVED, ReviewDecision.REJECTED);
  }

  private static Activity newActivity(UUID studentId, String description) {
    return Activity.builder()
        .studentId(studentId)
        .internshipId(INTERNSHIP_ID)
        .activityDate(LocalDate.of(2026, 9, 5))
        .description(description)
        .build();
  }

  private void setAuditor(UUID id) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, id.toString())
            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}
