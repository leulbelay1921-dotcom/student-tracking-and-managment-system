package com.sitms.progress;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link ProgressController} (SITMS-033): combined tasks + approved activities. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ProgressControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID CALLER_SUPERVISOR_APP_USER =
      UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID CALLER_SUPERVISOR_ID = UUID.fromString("12121212-1212-1212-1212-121212121212");
  private static final UUID OTHER_STUDENT_APP_USER =
      UUID.fromString("22222222-2222-2222-2222-222222222221");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(CALLER_SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID, STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID, STUDENT_APP_USER, ORGANIZATION_ID, STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-2', 'Sup Caller', ?)",
        CALLER_SUPERVISOR_ID, CALLER_SUPERVISOR_APP_USER, ORGANIZATION_ID, STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID, STUDENT_APP_USER, STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID, OTHER_STUDENT_APP_USER, OTHER_STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID, STUDENT_ID, ORGANIZATION_ID, SUPERVISOR_ID, STUDENT_APP_USER);
  }

  private void seedTask(UUID id, String status) {
    jdbcTemplate.update(
        "INSERT INTO task (id, student_id, internship_id, supervisor_id, title, status, created_by, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'Task', ?, ?, now(), now())",
        id, STUDENT_ID, INTERNSHIP_ID, SUPERVISOR_ID, status, STUDENT_APP_USER);
  }

  private void seedActivity(UUID id, String status) {
    jdbcTemplate.update(
        "INSERT INTO activity (id, student_id, internship_id, activity_date, type, description, status, created_by, created_at, updated_at) "
            + "VALUES (?, ?, ?, '2026-09-01', 'DAILY', 'desc', ?, ?, now(), now())",
        id, STUDENT_ID, INTERNSHIP_ID, status, STUDENT_APP_USER);
  }

  @Test
  void tasksOnlyDriveProgress() throws Exception {
    seedTask(UUID.randomUUID(), "COMPLETED");
    seedTask(UUID.randomUUID(), "PENDING");

    mockMvc
        .perform(get("/progress").with(auth("admin", "ADMIN")).param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalTasks").value(2))
        .andExpect(jsonPath("$.data.completedTasks").value(1))
        .andExpect(jsonPath("$.data.attemptedActivities").value(0))
        .andExpect(jsonPath("$.data.percent").value(50.0));
  }

  @Test
  void approvedActivitiesDriveProgress() throws Exception {
    seedActivity(UUID.randomUUID(), "APPROVED");
    seedActivity(UUID.randomUUID(), "SUBMITTED");

    mockMvc
        .perform(get("/progress").with(auth("admin", "ADMIN")).param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalTasks").value(0))
        .andExpect(jsonPath("$.data.approvedActivities").value(1))
        .andExpect(jsonPath("$.data.attemptedActivities").value(2))
        .andExpect(jsonPath("$.data.percent").value(50.0));
  }

  @Test
  void combinedAveragesTasksAndActivities() throws Exception {
    seedTask(UUID.randomUUID(), "COMPLETED");
    seedActivity(UUID.randomUUID(), "APPROVED");

    mockMvc
        .perform(get("/progress").with(auth("admin", "ADMIN")).param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.percent").value(100.0));
  }

  @Test
  void emptyReturnsZeroPercent() throws Exception {
    mockMvc
        .perform(get("/progress").with(auth("admin", "ADMIN")).param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.percent").value(0.0));
  }

  @Test
  void studentProgressDefaultsToOwnProfile() throws Exception {
    seedTask(UUID.randomUUID(), "COMPLETED");

    mockMvc
        .perform(get("/progress").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.totalTasks").value(1));
  }

  @Test
  void studentCannotReadAnotherStudentsProgress() throws Exception {
    mockMvc
        .perform(
            get("/progress")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .param("studentId", UUID.randomUUID().toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorCanViewAnotherSupervisorsStudentProgress() throws Exception {
    seedTask(UUID.randomUUID(), "COMPLETED");

    mockMvc
        .perform(
            get("/progress")
                .with(auth(CALLER_SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", SUPERVISOR_ID.toString())
                .param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.totalTasks").value(1));
  }

  @Test
  void supervisorCannotViewStudentNotAssignedToPickedSupervisor() throws Exception {
    mockMvc
        .perform(
            get("/progress")
                .with(auth(CALLER_SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", SUPERVISOR_ID.toString())
                .param("studentId", OTHER_STUDENT_ID.toString()))
        .andExpect(status().isForbidden());
  }

  private RequestPostProcessor auth(String sub, String... roles) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, sub)
            .claim("preferred_username", "user-" + sub)
            .claim("realm_access", Map.of("roles", List.of(roles)))
            .build();
    return authentication(jwtAuthenticationConverter.convert(jwt));
  }

  private void seedAppUser(UUID id, String username, String role) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, ?, TRUE, ?)",
        id, username, username + "@example.com", username, role, id);
  }
}
