package com.sitms.evaluation;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.config.JpaAuditingConfig;
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
 * Repository integration test for {@link Evaluation} against a real Postgres (SITMS-040):
 * persist/find round-trip, computed overall score, audit columns, and student/supervisor/internship
 * queries.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class EvaluationRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_STUDENT_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private EvaluationRepository evaluationRepository;
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

  private UUID seedInternship(UUID studentId) {
    UUID internshipId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        internshipId,
        studentId,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        AUDITOR_ID);
    return internshipId;
  }

  @Test
  void persistsAndRetrievesEvaluation() {
    setAuditor(AUDITOR_ID);
    Evaluation saved =
        evaluationRepository.save(
            newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 5, 4, 4, 5)
                .strengths("Solid work")
                .improvement("More unit tests")
                .comments("Keep it up")
                .status(EvaluationStatus.DRAFT)
                .build());

    Evaluation found = evaluationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStudentId()).isEqualTo(STUDENT_ID);
    assertThat(found.getSupervisorId()).isEqualTo(SUPERVISOR_ID);
    assertThat(found.getTechnicalSkills()).isEqualTo(4);
    assertThat(found.getCommunication()).isEqualTo(5);
    assertThat(found.getTeamwork()).isEqualTo(4);
    assertThat(found.getProblemSolving()).isEqualTo(4);
    assertThat(found.getProfessionalism()).isEqualTo(5);
    assertThat(found.getStrengths()).isEqualTo("Solid work");
    assertThat(found.getImprovement()).isEqualTo("More unit tests");
    assertThat(found.getComments()).isEqualTo("Keep it up");
    assertThat(found.getOverallScore()).isEqualTo(88);
    assertThat(found.getStatus()).isEqualTo(EvaluationStatus.DRAFT);
  }

  @Test
  void computesOverallScoreFromDimensionScores() {
    setAuditor(AUDITOR_ID);
    assertThat(Evaluation.computeOverallScore(1, 1, 1, 1, 1)).isEqualTo(20);
    assertThat(Evaluation.computeOverallScore(5, 5, 5, 5, 5)).isEqualTo(100);
    assertThat(Evaluation.computeOverallScore(4, 5, 4, 4, 5)).isEqualTo(88);
    assertThat(Evaluation.computeOverallScore(3, 3, 3, 3, 4)).isEqualTo(64);
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Evaluation saved =
        evaluationRepository.save(
            newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).build());
    evaluationRepository.flush();

    Evaluation found = evaluationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByStudentIdPaged() {
    setAuditor(AUDITOR_ID);
    evaluationRepository.save(newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).build());
    evaluationRepository.save(newEvaluation(STUDENT_ID, SUPERVISOR_ID, 5, 5, 5, 5, 5).build());
    evaluationRepository.save(newEvaluation(OTHER_STUDENT_ID, SUPERVISOR_ID, 3, 3, 3, 3, 3).build());

    Page<Evaluation> page =
        evaluationRepository.findByStudentId(STUDENT_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
    assertThat(page.getContent()).allMatch(e -> e.getStudentId().equals(STUDENT_ID));
  }

  @Test
  void findsByStudentIdAndStatusFiltersDrafts() {
    setAuditor(AUDITOR_ID);
    evaluationRepository.save(
        newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4)
            .status(EvaluationStatus.DRAFT)
            .build());
    evaluationRepository.save(
        newEvaluation(STUDENT_ID, SUPERVISOR_ID, 5, 5, 5, 5, 5)
            .status(EvaluationStatus.SUBMITTED)
            .build());
    evaluationRepository.save(
        newEvaluation(OTHER_STUDENT_ID, SUPERVISOR_ID, 3, 3, 3, 3, 3)
            .status(EvaluationStatus.SUBMITTED)
            .build());

    Page<Evaluation> page =
        evaluationRepository.findByStudentIdAndStatus(
            STUDENT_ID, EvaluationStatus.SUBMITTED, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(1);
    assertThat(page.getContent().get(0).getStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
  }

  @Test
  void findsByStudentIdAndInternshipId() {
    setAuditor(AUDITOR_ID);
    UUID internshipId = seedInternship(STUDENT_ID);
    evaluationRepository.save(
        newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).internshipId(internshipId).build());

    assertThat(evaluationRepository.findByStudentIdAndInternshipId(STUDENT_ID, internshipId))
        .hasSize(1);
    assertThat(evaluationRepository.existsByStudentIdAndInternshipId(STUDENT_ID, internshipId))
        .isTrue();
  }

  @Test
  void findsBySupervisorIdPaged() {
    setAuditor(AUDITOR_ID);
    evaluationRepository.save(newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).build());
    evaluationRepository.save(newEvaluation(OTHER_STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).build());

    Page<Evaluation> page =
        evaluationRepository.findBySupervisorId(SUPERVISOR_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findsByInternshipId() {
    setAuditor(AUDITOR_ID);
    UUID internshipId = seedInternship(STUDENT_ID);
    evaluationRepository.save(
        newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).internshipId(internshipId).build());
    evaluationRepository.save(newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4).build());

    assertThat(evaluationRepository.findByInternshipId(internshipId)).hasSize(1);
  }

  @Test
  void updatesEvaluationAndRecomputesScore() {
    setAuditor(AUDITOR_ID);
    Evaluation saved =
        evaluationRepository.saveAndFlush(
            newEvaluation(STUDENT_ID, SUPERVISOR_ID, 4, 4, 4, 4, 4)
                .status(EvaluationStatus.DRAFT)
                .build());

    saved.update(
        EvaluationStatus.SUBMITTED, 5, 5, 5, 5, 5, "great", null, "well done",
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
    evaluationRepository.saveAndFlush(saved);

    Evaluation found = evaluationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
    assertThat(found.getTechnicalSkills()).isEqualTo(5);
    assertThat(found.getStrengths()).isEqualTo("great");
    assertThat(found.getComments()).isEqualTo("well done");
    assertThat(found.getOverallScore()).isEqualTo(100);
  }

  private static Evaluation.Builder newEvaluation(
      UUID studentId, UUID supervisorId, int t, int c, int tw, int ps, int p) {
    return Evaluation.builder()
        .studentId(studentId)
        .supervisorId(supervisorId)
        .technicalSkills(t)
        .communication(c)
        .teamwork(tw)
        .problemSolving(ps)
        .professionalism(p);
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