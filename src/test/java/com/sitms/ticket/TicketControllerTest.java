package com.sitms.ticket;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Integration tests for {@link TicketController} (SITMS-073): raising, updating, scoping. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class TicketControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("99999999-9999-9999-9999-999999999991");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999992");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID FACULTY_APP_USER = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID FACULTY_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_INTERNSHIP_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        STUDENT_APP_USER);
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_APP_USER,
        STUDENT_APP_USER);
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_APP_USER,
        OTHER_STUDENT_APP_USER);
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        SUPERVISOR_APP_USER);
    seedAppUser(FACULTY_APP_USER, "faculty1", "FACULTY");
    jdbcTemplate.update(
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, created_by) "
            + "VALUES (?, ?, 'FAC-1', 'Prof One', ?)",
        FACULTY_ID,
        FACULTY_APP_USER,
        FACULTY_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id,"
            + " title, status, created_by) VALUES (?, ?, ?, ?, ?, 'Dev Intern', 'APPROVED', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        FACULTY_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, title, status, created_by) "
            + "VALUES (?, ?, ?, 'Other Intern', 'APPROVED', ?)",
        OTHER_INTERNSHIP_ID,
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
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

  private static String createBody(UUID internshipId) {
    return "{\"internshipId\":\"" + internshipId + "\",\"title\":\"Issue with login\","
        + "\"description\":\"Cannot log into the portal\",\"priority\":\"HIGH\"}";
  }

  private UUID createTicket(UUID internshipId, String sub, String role) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/tickets")
                    .with(auth(sub, role))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody(internshipId)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(
        new com.fasterxml.jackson.databind.ObjectMapper().readTree(response)
            .path("data").path("id").asText());
  }

  @Test
  void anonymousCannotList() throws Exception {
    mockMvc.perform(get("/tickets")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentRaisesTicketForOwnInternship() throws Exception {
    mockMvc
        .perform(
            post("/tickets").with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(INTERNSHIP_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("OPEN"))
        .andExpect(jsonPath("$.data.priority").value("HIGH"))
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()));
  }

  @Test
  void studentCannotRaiseTicketForAnotherStudentsInternship() throws Exception {
    mockMvc
        .perform(
            post("/tickets").with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(OTHER_INTERNSHIP_ID)))
        .andExpect(status().isForbidden());
  }

  @Test
    void supervisorCanRaiseTicketForAssignedStudent() throws Exception {
    mockMvc
        .perform(
            post("/tickets").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(INTERNSHIP_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.assigneeType").value("SUPERVISOR"))
        .andExpect(jsonPath("$.data.assigneeId").value(SUPERVISOR_ID.toString()));
    mockMvc
        .perform(
            post("/tickets").with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody(INTERNSHIP_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.assigneeType").value("FACULTY"))
        .andExpect(jsonPath("$.data.assigneeId").value(FACULTY_ID.toString()));
  }

  @Test
  void studentCannotUpdateTicket() throws Exception {
    UUID id = createTicket(INTERNSHIP_ID, STUDENT_APP_USER.toString(), "STUDENT");
    mockMvc
        .perform(
            patch("/tickets/" + id).with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorMovesTicketThroughLifecycle() throws Exception {
    UUID id = createTicket(INTERNSHIP_ID, STUDENT_APP_USER.toString(), "STUDENT");
    mockMvc
        .perform(
            patch("/tickets/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    mockMvc
        .perform(
            patch("/tickets/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("RESOLVED"));
  }

  @Test
    void facultyUpdatesTicketForAssignedStudent() throws Exception {
    UUID id = createTicket(INTERNSHIP_ID, STUDENT_APP_USER.toString(), "STUDENT");
    mockMvc
        .perform(
            patch("/tickets/" + id).with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"RESOLVED\",\"assigneeType\":\"FACULTY\","
                    + "\"assigneeId\":\"" + FACULTY_ID + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("RESOLVED"))
        .andExpect(jsonPath("$.data.assigneeType").value("FACULTY"));
  }

  @Test
  void refusingTransitionFromTerminalIsRejected() throws Exception {
    UUID id = createTicket(INTERNSHIP_ID, STUDENT_APP_USER.toString(), "STUDENT");
    mockMvc
        .perform(
            patch("/tickets/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CLOSED\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            patch("/tickets/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OPEN\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(TicketService.INVALID_TICKET_TRANSITION));
  }
}