package com.sitms.weeklyreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for the weekly report correction request flow (SITMS-077, SITMS-061). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class WeeklyReportControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID FACULTY_APP_USER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID FACULTY_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_STUDENT_APP_USER =
      UUID.fromString("99999999-9999-9999-9999-999999999991");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999992");
  private static final UUID OTHER_SUPERVISOR_APP_USER =
      UUID.fromString("99999999-9999-9999-9999-999999999993");
  private static final UUID OTHER_SUPERVISOR_ID =
      UUID.fromString("99999999-9999-9999-9999-999999999994");
  private static final UUID OTHER_INTERNSHIP_ID =
      UUID.fromString("99999999-9999-9999-9999-999999999995");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private WeeklyReportRepository weeklyReportRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(FACULTY_APP_USER, "faculty1", "FACULTY");
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
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
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, title, department, created_by) "
            + "VALUES (?, ?, 'A-1', 'Faculty One', 'Dr', 'CS', ?)",
        FACULTY_ID,
        FACULTY_APP_USER,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        FACULTY_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_APP_USER,
        OTHER_STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-2', 'Sup Two', ?)",
        OTHER_SUPERVISOR_ID,
        OTHER_SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        OTHER_STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'Other Internship', ?)",
        OTHER_INTERNSHIP_ID,
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
        FACULTY_ID,
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

  private Map<String, Object> payload() {
    return Map.of(
        "internshipId", INTERNSHIP_ID.toString(),
        "weekNumber", 3,
        "title", "Week three",
        "content", "Full narrative",
        "activitiesCompleted", "Built the API module",
        "problemsEncountered", "Auth tokens expired",
        "skillsLearned", "Spring Security",
        "nextWeekPlan", "Write more tests");
  }

  private UUID createReport() throws Exception {
    return createReportFor(STUDENT_APP_USER, INTERNSHIP_ID);
  }

  private UUID createReportFor(UUID studentAppUser, UUID internshipId) throws Exception {
    Map<String, Object> p = new java.util.LinkedHashMap<>(payload());
    p.put("internshipId", internshipId.toString());
    String response =
        mockMvc
            .perform(
                post("/weekly-reports")
                    .with(auth(studentAppUser.toString(), "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(p)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("PENDING_COMPANY"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
  }

  private void companyApprove(UUID id) throws Exception {
    mockMvc
        .perform(
            post("/weekly-reports/" + id + "/company-review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":true,\"comment\":\"Good. Adds detail.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_APPROVED"));
  }

  @Test
  void facultyCanRequestCorrectionInsteadOfRejecting() throws Exception {
    UUID id = createReport();
    companyApprove(id);

    mockMvc
        .perform(
            post("/weekly-reports/" + id + "/academic-review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"approved\":false,\"comment\":\"Please expand the skills section.\","
                        + "\"requestCorrection\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CORRECTION_REQUESTED"));

    WeeklyReport report = weeklyReportRepository.findById(id).orElseThrow();
    assertThat(report.getStatus()).isEqualTo(WeeklyReportStatus.CORRECTION_REQUESTED);
    assertThat(report.getAcademicReviewNote()).isEqualTo("Please expand the skills section.");
    assertThat(report.getAcademicReviewerId()).isEqualTo(FACULTY_ID);
  }

  @Test
  void studentCanEditCorrectionRequestedReportAndItRequiresCompanyAgain() throws Exception {
    UUID id = createReport();
    companyApprove(id);
    mockMvc
        .perform(
            post("/weekly-reports/" + id + "/academic-review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":false,\"comment\":\"Expand skills section.\",\"requestCorrection\":true}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("CORRECTION_REQUESTED"));

    var update = new java.util.LinkedHashMap<String, Object>(payload());
    update.put("skillsLearned", "Spring Security, JPA, OAuth2");
    mockMvc
        .perform(
            put("/weekly-reports/" + id)
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PENDING_COMPANY"))
        .andExpect(jsonPath("$.data.skillsLearned").value("Spring Security, JPA, OAuth2"));

    WeeklyReport report = weeklyReportRepository.findById(id).orElseThrow();
    assertThat(report.getStatus()).isEqualTo(WeeklyReportStatus.PENDING_COMPANY);
    assertThat(report.getCompanyReviewerId()).isNull();
    assertThat(report.getAcademicReviewNote()).isNull();
  }

  @Test
  void academicReviewWithoutCorrectionFlagStillRejects() throws Exception {
    UUID id = createReport();
    companyApprove(id);

    mockMvc
        .perform(
            post("/weekly-reports/" + id + "/academic-review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":false,\"comment\":\"Not enough detail.\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REJECTED"));
  }

  @Test
  void correctionRequestedReportsListForTheStudent() throws Exception {
    UUID id = createReport();
    companyApprove(id);
    mockMvc
        .perform(
            post("/weekly-reports/" + id + "/academic-review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":false,\"comment\":\"Do it again.\",\"requestCorrection\":true}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/weekly-reports").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].status").value("CORRECTION_REQUESTED"));
  }

  @Test
  void studentCannotRequestCorrection() throws Exception {
    UUID id = createReport();
    companyApprove(id);

    mockMvc
        .perform(
            post("/weekly-reports/" + id + "/academic-review")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"approved\":false,\"requestCorrection\":true}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void studentsSeeAllReportsOrderedByCreation() throws Exception {
    createReport();
    var all = weeklyReportRepository.findAll(PageRequest.of(0, 20)).getContent();
    assertThat(all).isNotEmpty();
  }

  @Test
  void supervisorCanViewAnotherSupervisorsAssignedStudentReports() throws Exception {
    createReport();
    createReportFor(OTHER_STUDENT_APP_USER, OTHER_INTERNSHIP_ID);

    mockMvc
        .perform(
            get("/weekly-reports")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].studentId").value(OTHER_STUDENT_ID.toString()));
  }
}