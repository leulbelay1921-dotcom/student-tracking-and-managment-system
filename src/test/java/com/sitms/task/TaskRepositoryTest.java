package com.sitms.task;

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
 * Repository integration test for {@link Task} against a real Postgres (SITMS-023): persist/find,
 * audit columns, reference + status queries, and status lifecycle mutators.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class TaskRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_STUDENT_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID INTERNSHIP_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");

  @Autowired private TaskRepository taskRepository;
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
  void persistsAndRetrievesTask() {
    setAuditor(AUDITOR_ID);
    Task saved =
        taskRepository.save(
            Task.builder()
                .studentId(STUDENT_ID)
                .internshipId(INTERNSHIP_ID)
                .supervisorId(SUPERVISOR_ID)
                .title("Write unit tests")
                .description("Cover task entity")
                .dueDate(LocalDate.of(2026, 9, 10))
                .build());

    Task found = taskRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getTitle()).isEqualTo("Write unit tests");
    assertThat(found.getStudentId()).isEqualTo(STUDENT_ID);
    assertThat(found.getInternshipId()).isEqualTo(INTERNSHIP_ID);
    assertThat(found.getSupervisorId()).isEqualTo(SUPERVISOR_ID);
    assertThat(found.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    assertThat(found.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Task saved = taskRepository.save(newTask(STUDENT_ID, "A").build());
    taskRepository.flush();

    Task found = taskRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByStudentPaged() {
    setAuditor(AUDITOR_ID);
    taskRepository.save(newTask(STUDENT_ID, "A").build());
    taskRepository.save(newTask(STUDENT_ID, "B").build());
    taskRepository.save(newTask(OTHER_STUDENT_ID, "C").build());

    Page<Task> page = taskRepository.findByStudentId(STUDENT_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);
  }

  @Test
  void findsByInternshipAndSupervisorPaged() {
    setAuditor(AUDITOR_ID);
    taskRepository.save(newTask(STUDENT_ID, "A").build());

    Page<Task> byInternship =
        taskRepository.findByInternshipId(INTERNSHIP_ID, PageRequest.of(0, 10));
    assertThat(byInternship.getTotalElements()).isEqualTo(1);

    Page<Task> bySupervisor = taskRepository.findBySupervisorId(SUPERVISOR_ID, PageRequest.of(0, 10));
    assertThat(bySupervisor.getTotalElements()).isEqualTo(1);
  }

  @Test
  void countsCompletedForProgress() {
    setAuditor(AUDITOR_ID);
    taskRepository.save(newTask(STUDENT_ID, "A").build());
    taskRepository.save(
        newTask(STUDENT_ID, "B").status(TaskStatus.COMPLETED).build());
    taskRepository.save(
        newTask(STUDENT_ID, "C").status(TaskStatus.IN_PROGRESS).build());

    assertThat(taskRepository.countByStudentId(STUDENT_ID)).isEqualTo(3);
    assertThat(taskRepository.countByStudentIdAndStatus(STUDENT_ID, TaskStatus.COMPLETED))
        .isEqualTo(1);
  }

  @Test
  void transitionsStatusOnSave() {
    setAuditor(AUDITOR_ID);
    Task saved = taskRepository.saveAndFlush(newTask(STUDENT_ID, "A").build());
    saved.changeStatus(TaskStatus.COMPLETED);
    taskRepository.saveAndFlush(saved);

    Task found = taskRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(TaskStatus.COMPLETED);
  }

  private static Task.Builder newTask(UUID studentId, String title) {
    return Task.builder()
        .studentId(studentId)
        .internshipId(INTERNSHIP_ID)
        .supervisorId(SUPERVISOR_ID)
        .title(title);
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
