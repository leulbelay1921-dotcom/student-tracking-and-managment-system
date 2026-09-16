package com.sitms.academicevaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Integration tests for the academic evaluation upsert and role scoping (SITMS-078).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AcademicEvaluationControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID FACULTY_APP_USER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID FACULTY_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  private static final UUID OTHER_FACULTY_APP_USER = UUID.fromString("abcdabcd-abcd-abcd-abcd-abcdabcdabcd");
  private static final UUID OTHER_FACULTY_ID = UUID.fromString("dcbadcba-dcba-dcba-dcba-dcbadcbadcba");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_INTERNSHIP_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AcademicEvaluationRepository academicEvaluationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(FACULTY_APP_USER, "faculty1", "FACULTY");
    seedAppUser(OTHER_FACULTY_APP_USER, "faculty2", "FACULTY");
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
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, title, department, created_by) "
            + "VALUES (?, ?, 'A-2', 'Faculty Two', 'Dr', 'CS', ?)",
        OTHER_FACULTY_ID,
        OTHER_FACULTY_APP_USER,
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
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'Internship', ?)",
        OTHER_INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        OTHER_FACULTY_ID,
        STUDENT_APP_USER);
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

  private String payload(UUID internshipId) {
    return """
        {"internshipId":"%s","studentId":"%s","academicLearning":5,"problemSolving":4,\
        "documentation":3,"technicalDevelopment":4,"professionalism":5,"finalPresentation":3,\
        "remarks":"Solid final defense."}""".formatted(internshipId, STUDENT_ID);
  }

  @Test
  void facultySavesAcademicEvaluationForAssignedInternship() throws Exception {
    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(INTERNSHIP_ID)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.evaluatorId").value(FACULTY_ID.toString()))
        .andExpect(jsonPath("$.data.academicLearning").value(5))
        .andExpect(jsonPath("$.data.finalPresentation").value(3))
        .andExpect(jsonPath("$.data.totalScore").value(80));

    AcademicEvaluation saved =
        academicEvaluationRepository.findByInternshipId(INTERNSHIP_ID).orElseThrow();
    assertThat(saved.getEvaluatorId()).isEqualTo(FACULTY_ID);
    assertThat(saved.getRemarks()).isEqualTo("Solid final defense.");
  }

  @Test
  void facultyCannotEvaluateAnInternshipAssignedToAnotherAcademicStaff() throws Exception {
    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(OTHER_INTERNSHIP_ID)))
        .andExpect(status().isForbidden());
  }

  @Test
  void savingTwiceUpdatesInsteadOfDuplicating() throws Exception {
    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(INTERNSHIP_ID)))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    payload(INTERNSHIP_ID)
                        .replace("\"academicLearning\":5", "\"academicLearning\":2")
                        .replace("\"remarks\":\"Solid final defense.\"",
                            "\"remarks\":\"Revised after retake.\"")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.academicLearning").value(2))
        .andExpect(jsonPath("$.data.remarks").value("Revised after retake."))
        .andExpect(jsonPath("$.data.totalScore").value(70));

    assertThat(academicEvaluationRepository.findByInternshipId(INTERNSHIP_ID)).isPresent();
    assertThat(academicEvaluationRepository.findAll()).hasSize(1);
  }

  @Test
  void adminCanSaveUsingAnExplicitEvaluator() throws Exception {
    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    payload(OTHER_INTERNSHIP_ID)
                        .replaceFirst(
                            "\\{\"internshipId\"",
                            "{\"evaluatorId\":\"" + OTHER_FACULTY_ID + "\",\"internshipId\"")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.evaluatorId").value(OTHER_FACULTY_ID.toString()));
  }

  @Test
  void studentSeesOwnAcademicEvaluationInList() throws Exception {
    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(INTERNSHIP_ID)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/academic-evaluations").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].internshipId").value(INTERNSHIP_ID.toString()));
  }

  @Test
  void byInternshipReturnsNullWhenNoEvaluationExists() throws Exception {
    mockMvc
        .perform(
            get("/academic-evaluations/by-internship/" + INTERNSHIP_ID)
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").doesNotExist());
  }

  @Test
  void studentCannotSaveAnAcademicEvaluation() throws Exception {
    mockMvc
        .perform(
            put("/academic-evaluations")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload(INTERNSHIP_ID)))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorIsDeniedFromAcademicEvaluations() throws Exception {
    mockMvc
        .perform(
            get("/academic-evaluations")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isForbidden());
  }
}