package com.sitms.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.sitms.settings.AppSettingRequest;
import com.sitms.settings.AppSettingService;
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

/** Integration tests for {@link ApplicationController} (SITMS M2): apply, decide, withdraw. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ApplicationControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID OTHER_SUPERVISOR_APP_USER = UUID.fromString("ccccccc1-cccc-cccc-cccc-cccccccccccc");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("ddddddd1-dddd-dddd-dddd-dddddddddddd");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AppSettingService appSettingService;
  @Autowired private ApplicationRepository applicationRepository;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    appSettingService.update("registration_open", new AppSettingRequest("true"));
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(OTHER_SUPERVISOR_APP_USER, "supervisor2", "SUPERVISOR");
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
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-2', 'Sup Two', ?)",
        OTHER_SUPERVISOR_ID,
        OTHER_SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_APP_USER);
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

  private String applicationPayload() {
    return "{\"organizationId\":\"" + ORGANIZATION_ID
        + "\",\"title\":\"Backend Internship\",\"description\":\"API work\","
        + "\"startDate\":\"2026-10-01\",\"endDate\":\"2026-12-31\"}";
  }

  private String submitFor(String sub) throws Exception {
    String body =
        mockMvc
            .perform(
                post("/applications")
                    .with(auth(sub, "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(applicationPayload()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.parse(body).read("$.data.id", String.class);
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
  void anonymousCannotListApplications() throws Exception {
    mockMvc.perform(get("/applications")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentSubmitsPendingApplication() throws Exception {
    mockMvc
        .perform(
            post("/applications")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationPayload()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.organizationId").value(ORGANIZATION_ID.toString()))
        .andExpect(jsonPath("$.data.status").value("PENDING"));
  }

  @Test
  void applicationRequiresOrganizationAndTitle() throws Exception {
    mockMvc
        .perform(
            post("/applications")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Missing org\"}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void studentCannotHoldTwoActiveApplications() throws Exception {
    submitFor(STUDENT_APP_USER.toString());

    mockMvc
        .perform(
            post("/applications")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationPayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ApplicationService.APPLICATION_ALREADY_EXISTS));
  }

  @Test
  void studentCannotApplyWithApprovedOrCompletedPlacement() throws Exception {
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, created_by) "
            + "VALUES (?, ?, ?, ?, 'Approved Posting', 'APPROVED', ?)",
        UUID.randomUUID(),
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        STUDENT_APP_USER);

    mockMvc
        .perform(
            post("/applications")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(applicationPayload()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ApplicationService.APPLICATION_ALREADY_EXISTS));
  }

  @Test
  void adminApprovingApplicationCreatesPlacement() throws Exception {
    String id = submitFor(STUDENT_APP_USER.toString());

    mockMvc
        .perform(
            patch("/applications/" + id + "/status")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("APPROVED"));

    applicationRepository.flush();
    Integer placements =
        jdbcTemplate.queryForObject(
            "SELECT count(*) FROM internship WHERE student_id = ? AND status = 'APPROVED'",
            Integer.class,
            STUDENT_ID);
    assertThat(placements).isEqualTo(1);
  }

  @Test
  void supervisorCanDecideAssignedStudentsApplication() throws Exception {
    String id = submitFor(STUDENT_APP_USER.toString());

    mockMvc
        .perform(
            patch("/applications/" + id + "/status")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REJECTED"));
  }

  @Test
  void unassignedSupervisorCannotDecideApplication() throws Exception {
    String id = submitFor(STUDENT_APP_USER.toString());

    mockMvc
        .perform(
            patch("/applications/" + id + "/status")
                .with(auth(OTHER_SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void studentWithdrawsOwnPendingApplication() throws Exception {
    String id = submitFor(STUDENT_APP_USER.toString());

    mockMvc
        .perform(post("/applications/" + id + "/withdraw").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("WITHDRAWN"));
  }

  @Test
  void studentCannotWithdrawAnotherStudentsApplication() throws Exception {
    String id = submitFor(STUDENT_APP_USER.toString());

    mockMvc
        .perform(post("/applications/" + id + "/withdraw").with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(ApplicationService.APPLICATION_NOT_FOUND));
  }

  @Test
  void decidedApplicationCannotBeDecidedAgain() throws Exception {
    String id = submitFor(STUDENT_APP_USER.toString());
    mockMvc
        .perform(
            patch("/applications/" + id + "/status")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/applications/" + id + "/status")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REJECTED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ApplicationService.INVALID_STATUS_TRANSITION));
  }

  @Test
  void studentSeesOnlyOwnApplications() throws Exception {
    submitFor(STUDENT_APP_USER.toString());
    submitFor(OTHER_STUDENT_APP_USER.toString());

    mockMvc
        .perform(get("/applications").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mockMvc
        .perform(
            get("/applications")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .param("studentId", OTHER_STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void supervisorSeesOnlyAssignedStudentsApplications() throws Exception {
    submitFor(STUDENT_APP_USER.toString());
    submitFor(OTHER_STUDENT_APP_USER.toString());

    mockMvc
        .perform(get("/applications").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void supervisorCanViewAnotherSupervisorsAssignedStudentsApplications() throws Exception {
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Other Internship', ?)",
        UUID.randomUUID(),
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
        OTHER_SUPERVISOR_APP_USER);
    submitFor(STUDENT_APP_USER.toString());
    submitFor(OTHER_STUDENT_APP_USER.toString());

    mockMvc
        .perform(
            get("/applications")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].studentId").value(OTHER_STUDENT_ID.toString()));
  }

  @Test
  void adminSeesAllApplications() throws Exception {
    submitFor(STUDENT_APP_USER.toString());
    submitFor(OTHER_STUDENT_APP_USER.toString());

    mockMvc
        .perform(get("/applications").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }
}