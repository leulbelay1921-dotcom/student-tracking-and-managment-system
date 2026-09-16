package com.sitms.audit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AuditLogController} (SITMS-056): admin-only reads + recorder wiring. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuditLogControllerTest {

  private static final UUID ADMIN_APP_USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID STUDENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

  private static final String ADMIN_USERNAME = "admin";

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seed() {
    seedAppUser(ADMIN_APP_USER, ADMIN_USERNAME, "ADMIN");
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(UUID.fromString("99999999-9999-9999-9999-999999999999"), "other", "SUPERVISOR");
    seedInternshipParents();
    seedAuditLog(ADMIN_APP_USER, "admin", "INTERNSHIP_STATUS_CHANGE", "-2 hours", "older");
    seedAuditLog(SUPERVISOR_APP_USER, SUPERVISOR_APP_USER.toString(), "ACTIVITY_REVIEWED", "-1 hours", "newer");
  }

  private void seedAppUser(UUID id, String username, String role) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, ?, TRUE, ?)",
        id,
        username,
        username + "@example.com",
        username,
        role,
        id);
  }

  private void seedInternshipParents() {
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        ADMIN_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_APP_USER,
        ADMIN_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        ADMIN_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, description,"
            + " status, created_by) VALUES (?, ?, ?, ?, 'SWE Internship', 'Frontend', 'PENDING', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        ADMIN_APP_USER);
  }

  private void seedAuditLog(UUID actorId, String actorName, String action, String offset, String detail) {
    java.sql.Timestamp ts = computeOffsetTimestamp(offset);
    jdbcTemplate.update(
        "INSERT INTO audit_log (id, actor_id, actor_name, action, target_type, target_id, detail,"
            + " created_by, created_at, updated_at) VALUES (?, ?, ?, ?, 'INTERNSHIP', ?, ?, ?,"
            + " ?, CURRENT_TIMESTAMP)",
        UUID.randomUUID(),
        actorId,
        actorName,
        action,
        INTERNSHIP_ID,
        detail,
        ADMIN_APP_USER,
        ts);
  }

  private static java.sql.Timestamp computeOffsetTimestamp(String offset) {
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    String trimmed = offset.trim();
    int value = Integer.parseInt(trimmed.split("\\s+")[0]);
    if (trimmed.contains("hour")) {
      now = now.plusHours(value);
    } else if (trimmed.contains("minute")) {
      now = now.plusMinutes(value);
    }
    return java.sql.Timestamp.valueOf(now);
  }

  private static Jwt buildJwt(String sub, String... roles) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim(JwtClaimNames.SUB, sub)
        .claim("preferred_username", "user-" + sub)
        .claim("realm_access", Map.of("roles", List.of(roles)))
        .build();
  }

  private RequestPostProcessor auth(String sub, String... roles) {
    return authentication(jwtAuthenticationConverter.convert(buildJwt(sub, roles)));
  }

  @Test
  void unauthenticatedCannotReadAuditLog() throws Exception {
    mockMvc.perform(get("/audit-logs")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCannotReadAuditLog() throws Exception {
    mockMvc
        .perform(get("/audit-logs").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorCannotReadAuditLog() throws Exception {
    mockMvc
        .perform(get("/audit-logs").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminListsAuditLogNewestFirst() throws Exception {
    mockMvc
        .perform(get("/audit-logs").with(auth(ADMIN_USERNAME, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items[0].action").value("ACTIVITY_REVIEWED"))
        .andExpect(jsonPath("$.data.items[1].action").value("INTERNSHIP_STATUS_CHANGE"))
        .andExpect(jsonPath("$.data.items[0].actorName").value(SUPERVISOR_APP_USER.toString()))
        .andExpect(jsonPath("$.data.items[0].targetType").value("INTERNSHIP"))
        .andExpect(jsonPath("$.data.items[0].targetId").value(INTERNSHIP_ID.toString()));
  }

  @Test
  void adminFiltersAuditLogByAction() throws Exception {
    mockMvc
        .perform(
            get("/audit-logs").param("action", "INTERNSHIP_STATUS_CHANGE")
                .with(auth(ADMIN_USERNAME, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].action").value("INTERNSHIP_STATUS_CHANGE"));
  }

  @Test
  void internshipStatusChangeIsRecordedAndVisibleToAdmin() throws Exception {
    mockMvc
        .perform(
            patch("/internships/" + INTERNSHIP_ID + "/status")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("APPROVED"));

    mockMvc
        .perform(get("/audit-logs").with(auth(ADMIN_USERNAME, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(3))
        .andExpect(jsonPath("$.data.items[0].action").value("INTERNSHIP_STATUS_CHANGE"))
        .andExpect(jsonPath("$.data.items[0].targetId").value(INTERNSHIP_ID.toString()))
        .andExpect(jsonPath("$.data.items[0].actorName").value(SUPERVISOR_APP_USER.toString()))
        .andExpect(jsonPath("$.data.items[0].actorId").value(SUPERVISOR_APP_USER.toString()))
        .andExpect(
            jsonPath("$.data.items[0].detail")
                .value("Internship status changed from PENDING to APPROVED."));
  }
}