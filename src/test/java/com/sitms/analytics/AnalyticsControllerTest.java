package com.sitms.analytics;

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

/** Integration tests for {@link AnalyticsController} (SITMS-035): aggregation + RBAC. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AnalyticsControllerTest {

  private static final UUID ORG_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID STUDENT_A_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_A_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID STUDENT_B_APP_USER = UUID.fromString("aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID STUDENT_B_ID = UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seed() {
    seedAppUser(STUDENT_A_APP_USER, "student1", "STUDENT");
    seedAppUser(STUDENT_B_APP_USER, "student2", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORG_ID, STUDENT_A_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID, STUDENT_A_APP_USER, ORG_ID, STUDENT_A_APP_USER);
    seedStudent(STUDENT_A_ID, STUDENT_A_APP_USER, "S-1", "CS");
    seedStudent(STUDENT_B_ID, STUDENT_B_APP_USER, "S-2", "BA");
  }

  private void seedStudent(UUID id, UUID appUserId, String studentNo, String program) {
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, ?, 'Student', ?, ?)",
        id, appUserId, studentNo, program, appUserId);
  }

  private void seedInternship(UUID id, UUID studentId, String status) {
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?, ?)",
        id, studentId, ORG_ID, SUPERVISOR_ID, status, STUDENT_A_APP_USER);
  }

  private void seedActivity(UUID id, UUID studentId, String activityDate, String status) {
    jdbcTemplate.update(
        "INSERT INTO activity (id, student_id, activity_date, type, description, status, created_by, created_at, updated_at) "
            + "VALUES (?, ?, CAST(? AS DATE), 'DAILY', 'desc', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
        id, studentId, activityDate, status, STUDENT_A_APP_USER);
  }

  @Test
  void summarizesStudentsInternshipsAndTrends() throws Exception {
    seedInternship(UUID.randomUUID(), STUDENT_A_ID, "APPROVED");
    seedInternship(UUID.randomUUID(), STUDENT_B_ID, "PENDING");
    seedActivity(UUID.randomUUID(), STUDENT_A_ID, "2026-08-31", "APPROVED");
    seedActivity(UUID.randomUUID(), STUDENT_A_ID, "2026-09-07", "SUBMITTED");

    mockMvc
        .perform(get("/analytics").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalStudents").value(2))
        .andExpect(jsonPath("$.data.studentsByProgram.CS").value(1))
        .andExpect(jsonPath("$.data.studentsByProgram.BA").value(1))
        .andExpect(jsonPath("$.data.internshipsByStatus.APPROVED").value(1))
        .andExpect(jsonPath("$.data.internshipsByStatus.PENDING").value(1))
        .andExpect(jsonPath("$.data.activityTrends[0].week").value("2026-08-31"))
        .andExpect(jsonPath("$.data.activityTrends[0].submitted").value(1))
        .andExpect(jsonPath("$.data.activityTrends[0].approved").value(1))
        .andExpect(jsonPath("$.data.activityTrends[1].week").value("2026-09-07"))
        .andExpect(jsonPath("$.data.activityTrends[1].submitted").value(1))
        .andExpect(jsonPath("$.data.activityTrends[1].approved").value(0));
  }

  @Test
  void blocksStudentsFromAnalytics() throws Exception {
    mockMvc
        .perform(get("/analytics").with(auth("stu", "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorSeesOnlyAssignedStudents() throws Exception {
    UUID otherSupId = UUID.fromString("ccccccc1-cccc-cccc-cccc-cccccccccccc");
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-2', 'Sup Two', ?)",
        otherSupId, STUDENT_B_APP_USER, ORG_ID, STUDENT_B_APP_USER);
    seedInternship(UUID.randomUUID(), STUDENT_A_ID, "APPROVED");
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?, ?)",
        UUID.randomUUID(), STUDENT_B_ID, ORG_ID, otherSupId, "PENDING", STUDENT_B_APP_USER);

    mockMvc
        .perform(get("/analytics").with(auth(STUDENT_A_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalStudents").value(1))
        .andExpect(jsonPath("$.data.studentsByProgram.CS").value(1))
        .andExpect(jsonPath("$.data.studentsByProgram.BA").doesNotExist())
        .andExpect(jsonPath("$.data.internshipsByStatus.APPROVED").value(1))
        .andExpect(jsonPath("$.data.internshipsByStatus.PENDING").value(0));
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
