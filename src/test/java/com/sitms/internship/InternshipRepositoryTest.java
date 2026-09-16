package com.sitms.internship;

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
 * Repository integration test for {@link Internship} against a real Postgres (SITMS-018):
 * persist/find, audit columns, status + reference queries, and status lifecycle mutators.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class InternshipRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID OTHER_STUDENT_ID =
      UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private InternshipRepository internshipRepository;
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

  @Test
  void persistsAndRetrievesInternship() {
    setAuditor(AUDITOR_ID);
    Internship saved =
        internshipRepository.save(
            Internship.builder()
                .studentId(STUDENT_ID)
                .organizationId(ORGANIZATION_ID)
                .supervisorId(SUPERVISOR_ID)
                .title("Software Engineering Internship")
                .description("Frontend work")
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2026, 9, 1))
                .build());

    Internship found = internshipRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getTitle()).isEqualTo("Software Engineering Internship");
    assertThat(found.getStudentId()).isEqualTo(STUDENT_ID);
    assertThat(found.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(found.getSupervisorId()).isEqualTo(SUPERVISOR_ID);
    assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2026, 6, 1));
    assertThat(found.getStatus()).isEqualTo(InternshipStatus.PENDING);
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Internship saved =
        internshipRepository.save(
            Internship.builder().studentId(STUDENT_ID).organizationId(ORGANIZATION_ID).title("T").build());
    internshipRepository.flush();

    Internship found = internshipRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByStudentIdPaged() {
    setAuditor(AUDITOR_ID);
    internshipRepository.save(newInternship(STUDENT_ID, "A"));
    internshipRepository.save(newInternship(STUDENT_ID, "B"));
    internshipRepository.save(newInternship(OTHER_STUDENT_ID, "C"));

    Page<Internship> page = internshipRepository.findByStudentId(STUDENT_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findsByStatusPaged() {
    setAuditor(AUDITOR_ID);
    internshipRepository.save(newInternship(STUDENT_ID, "A"));
    internshipRepository.save(
        Internship.builder()
            .studentId(OTHER_STUDENT_ID)
            .organizationId(ORGANIZATION_ID)
            .title("Approved")
            .status(InternshipStatus.APPROVED)
            .build());

    Page<Internship> pending =
        internshipRepository.findByStatus(InternshipStatus.PENDING, PageRequest.of(0, 10));
    assertThat(pending.getTotalElements()).isEqualTo(1);

    Page<Internship> approved =
        internshipRepository.findByStatus(InternshipStatus.APPROVED, PageRequest.of(0, 10));
    assertThat(approved.getTotalElements()).isEqualTo(1);
  }

  @Test
  void findsBySupervisorAndCountsApproved() {
    setAuditor(AUDITOR_ID);
    internshipRepository.save(newInternship(STUDENT_ID, "A"));
    internshipRepository.save(
        Internship.builder()
            .studentId(OTHER_STUDENT_ID)
            .organizationId(ORGANIZATION_ID)
            .supervisorId(SUPERVISOR_ID)
            .title("Approved")
            .status(InternshipStatus.APPROVED)
            .build());

    Page<Internship> bySupervisor =
        internshipRepository.findBySupervisorId(SUPERVISOR_ID, PageRequest.of(0, 10));
    assertThat(bySupervisor.getTotalElements()).isEqualTo(2);

    assertThat(
            internshipRepository.countBySupervisorIdAndStatus(
                SUPERVISOR_ID, InternshipStatus.APPROVED))
        .isEqualTo(1);
  }

  @Test
  void transitionsStatusAndReassignsSupervisorOnSave() {
    setAuditor(AUDITOR_ID);
    Internship saved = internshipRepository.saveAndFlush(newInternship(STUDENT_ID, "A"));
    saved.changeStatus(InternshipStatus.REJECTED);
    saved.assignSupervisor(SUPERVISOR_ID);
    internshipRepository.saveAndFlush(saved);

    Internship found = internshipRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(InternshipStatus.REJECTED);
    assertThat(found.getSupervisorId()).isEqualTo(SUPERVISOR_ID);
  }

  private static Internship newInternship(UUID studentId, String title) {
    return Internship.builder()
        .studentId(studentId)
        .organizationId(ORGANIZATION_ID)
        .supervisorId(SUPERVISOR_ID)
        .title(title)
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
