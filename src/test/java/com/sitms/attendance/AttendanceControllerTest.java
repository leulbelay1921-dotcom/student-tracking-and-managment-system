package com.sitms.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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

/** Integration tests for {@link AttendanceController} (SITMS-070): RBAC, record present/absent. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AttendanceControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID OTHER_SUPERVISOR_APP_USER =
      UUID.fromString("aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_APP_USER,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        SUPERVISOR_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        STUDENT_APP_USER);
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_APP_USER,
        OTHER_STUDENT_APP_USER);
    seedAppUser(OTHER_SUPERVISOR_APP_USER, "supervisor2", "SUPERVISOR");
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-2', 'Sup Two', ?)",
        OTHER_SUPERVISOR_ID,
        OTHER_SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        OTHER_STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Other Internship', ?)",
        UUID.randomUUID(),
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
        OTHER_STUDENT_APP_USER);
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

  private void seedAttendance(UUID studentId, String date, String status) {
    jdbcTemplate.update(
        "INSERT INTO attendance (id, student_id, attendance_date, status, created_by) "
            + "VALUES (?, ?, ?, ?, ?)",
        UUID.randomUUID(),
        studentId,
        LocalDate.parse(date),
        status,
        STUDENT_APP_USER);
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
  void anonymousCannotListAttendance() throws Exception {
    mockMvc.perform(get("/attendance")).andExpect(status().isUnauthorized());
  }

  @Test
  void supervisorRecordsPresentAndAbsentForAssignedStudent() throws Exception {
    mockMvc
        .perform(
            post("/attendance/record")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"studentId\":\"" + STUDENT_ID + "\",\"status\":\"PRESENT\","
                        + "\"note\":\"On site\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.status").value("PRESENT"));

    String otherDay = LocalDate.now().minusDays(1).toString();
    mockMvc
        .perform(
            post("/attendance/record")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"studentId\":\"" + STUDENT_ID + "\",\"status\":\"ABSENT\","
                        + "\"date\":\"" + otherDay + "\",\"note\":\"Sick leave\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("ABSENT"));

    Integer rows =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM attendance WHERE student_id = ?",
            Integer.class,
            STUDENT_ID);
    assertThat(rows).isEqualTo(2);
  }

  @Test
  void supervisorCannotRecordForUnassignedStudent() throws Exception {
    mockMvc
        .perform(
            post("/attendance/record")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"studentId\":\"" + OTHER_STUDENT_ID + "\",\"status\":\"PRESENT\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void studentCannotRecordAttendance() throws Exception {
    mockMvc
        .perform(
            post("/attendance/record")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":\"" + STUDENT_ID + "\",\"status\":\"PRESENT\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
    void adminCanReadButCannotWriteAttendance() throws Exception {
    seedAttendance(STUDENT_ID, "2026-09-01", "PRESENT");

    mockMvc
        .perform(get("/attendance").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/attendance/summary")
                .with(auth("admin", "ADMIN"))
                .param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/attendance/record")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":\"" + STUDENT_ID + "\",\"status\":\"PRESENT\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void summaryComputesRateFromRecordedRows() throws Exception {
    seedAttendance(STUDENT_ID, "2026-09-01", "PRESENT");
    seedAttendance(STUDENT_ID, "2026-09-02", "PRESENT");
    seedAttendance(STUDENT_ID, "2026-09-03", "ABSENT");

    mockMvc
        .perform(get("/attendance/summary").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalDays").value(3))
        .andExpect(jsonPath("$.data.presentDays").value(2))
        .andExpect(jsonPath("$.data.absentDays").value(1))
        .andExpect(jsonPath("$.data.rate").value(66.7));
  }

  @Test
  void summaryWithoutRecordsHasNullRate() throws Exception {
    mockMvc
        .perform(get("/attendance/summary").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalDays").value(0))
        .andExpect(jsonPath("$.data.rate").doesNotExist());
  }

  @Test
  void studentSeesOnlyOwnAttendance() throws Exception {
    seedAttendance(STUDENT_ID, "2026-09-01", "PRESENT");
    seedAttendance(OTHER_STUDENT_ID, "2026-09-01", "PRESENT");

    mockMvc
        .perform(get("/attendance").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mockMvc
        .perform(
            get("/attendance")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .param("studentId", OTHER_STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void studentSummaryForAnotherStudentIsForbidden() throws Exception {
    mockMvc
        .perform(
            get("/attendance/summary")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .param("studentId", OTHER_STUDENT_ID.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorSeesOnlyAssignedStudentAttendance() throws Exception {
    seedAttendance(STUDENT_ID, "2026-09-01", "ABSENT");
    seedAttendance(OTHER_STUDENT_ID, "2026-09-01", "PRESENT");

    mockMvc
        .perform(get("/attendance").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mockMvc
        .perform(
            get("/attendance/summary")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.absentDays").value(1));

    mockMvc
        .perform(
            get("/attendance/summary")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("studentId", OTHER_STUDENT_ID.toString()))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorCanViewAnotherSupervisorsAssignedStudentAttendance() throws Exception {
    seedAttendance(STUDENT_ID, "2026-09-01", "PRESENT");
    seedAttendance(OTHER_STUDENT_ID, "2026-09-01", "PRESENT");

    mockMvc
        .perform(
            get("/attendance")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].studentId").value(OTHER_STUDENT_ID.toString()));

    mockMvc
        .perform(
            get("/attendance/summary")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString())
                .param("studentId", OTHER_STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.absentDays").value(0));

    mockMvc
        .perform(
            get("/attendance/summary")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString())
                .param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isForbidden());
  }
}