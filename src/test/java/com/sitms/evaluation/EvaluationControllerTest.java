package com.sitms.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
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

/** Integration tests for {@link EvaluationController} (SITMS-041): CRUD, RBAC, scoping. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class EvaluationControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbb1");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID OTHER_SUPERVISOR_APP_USER = UUID.fromString("dddddddd-dddd-dddd-dddd-ddddddddddd1");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-ccccccccccc1");
  private static final UUID FACULTY_APP_USER = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee1");
  private static final UUID FACULTY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffff1");
  private static final UUID OTHER_FACULTY_APP_USER = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeee2");
  private static final UUID OTHER_FACULTY_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-fffffffffff2");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private EvaluationRepository evaluationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(OTHER_SUPERVISOR_APP_USER, "supervisor2", "SUPERVISOR");
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
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_APP_USER,
        OTHER_STUDENT_APP_USER);
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
            + "VALUES (?, ?, ?, ?, ?, 'Other Internship', ?)",
        UUID.randomUUID(),
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
        OTHER_FACULTY_ID,
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

  private String body(Map<String, Object> payload) {
    try {
      return objectMapper.writeValueAsString(payload);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Map<String, Object> request(UUID studentId, Integer score, Map<String, Object> extra) {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("studentId", studentId.toString());
    Map<String, Object> rest = new LinkedHashMap<>(extra);
    if (rest.containsKey("supervisorId")) {
      map.put("supervisorId", rest.remove("supervisorId"));
    }
    map.put("technicalSkills", score == null ? 4 : score);
    map.put("communication", 5);
    map.put("teamwork", 4);
    map.put("problemSolving", 4);
    map.put("professionalism", 5);
    map.putAll(rest);
    return map;
  }

  private UUID createEvaluation() throws Exception {
    String json =
        mockMvc
            .perform(
                post("/evaluations")
                    .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        body(
                            request(
                                STUDENT_ID,
                                null,
                                Map.of("internshipId", INTERNSHIP_ID.toString(), "strengths", "Solid work")))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(json).path("data").path("id").asText());
  }

  @Test
  void anonymousCannotListEvaluations() throws Exception {
    mockMvc.perform(get("/evaluations")).andExpect(status().isUnauthorized());
  }

  @Test
  void supervisorSeesAssignedStudentsInAssignees() throws Exception {
    mockMvc
        .perform(get("/evaluations/assignees").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student One"))
        .andExpect(jsonPath("$.data.items[0].studentNo").value("S-1"))
        .andExpect(jsonPath("$.data.items[0].internshipId").value(INTERNSHIP_ID.toString()))
        .andExpect(jsonPath("$.data.items[0].evaluationId").doesNotExist());
  }

  @Test
  void otherSupervisorSeesOwnAssignees() throws Exception {
    mockMvc
        .perform(get("/evaluations/assignees").with(auth(OTHER_SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student Two"));
  }

  @Test
  void supervisorCanViewAnotherSupervisorsAssignees() throws Exception {
    mockMvc
        .perform(
            get("/evaluations/assignees")
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString())
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student Two"));

    mockMvc
        .perform(get("/evaluations/assignees").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student One"));
  }

  @Test
  void adminWithoutSupervisorIdSeesAllAssignees() throws Exception {
    mockMvc
        .perform(get("/evaluations/assignees").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student One"))
        .andExpect(jsonPath("$.data.items[1].fullName").value("Student Two"));
  }

  @Test
  void adminSeesAssigneesForGivenSupervisor() throws Exception {
    mockMvc
        .perform(
            get("/evaluations/assignees")
                .param("supervisorId", SUPERVISOR_ID.toString())
                .with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student One"));
  }

  @Test
  void assigneeIncludesExistingEvaluationAfterCreate() throws Exception {
    createEvaluation();
    mockMvc
        .perform(get("/evaluations/assignees").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items[0].evaluationStatus").value("SUBMITTED"))
        .andExpect(jsonPath("$.data.items[0].evaluationId").isNotEmpty());
  }

  @Test
  void supervisorCreatesEvaluationForAssignedStudent() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        request(
                            STUDENT_ID,
                            null,
                            Map.of("strengths", "Solid work", "improvement", "More tests",
                                "comments", "Keep it up")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.supervisorId").value(SUPERVISOR_ID.toString()))
        .andExpect(jsonPath("$.data.technicalSkills").value(4))
        .andExpect(jsonPath("$.data.overallScore").value(88))
        .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
  }

  @Test
  void supervisorCannotEvaluateUnassignedStudent() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(OTHER_STUDENT_ID, null, Map.of()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorWithoutProfileCannotCreate() throws Exception {
    var noProfile = UUID.fromString("99999999-9999-9999-9999-999999999999");
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(noProfile.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, null, Map.of()))))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("SUPERVISOR_PROFILE_NOT_FOUND"));
  }

  @Test
  void studentCannotCreateEvaluation() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, null, Map.of()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesEvaluationWithSupervisorId() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        request(
                            OTHER_STUDENT_ID,
                            3,
                            Map.of("supervisorId", SUPERVISOR_ID.toString())))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.supervisorId").value(SUPERVISOR_ID.toString()));
  }

  @Test
  void adminCreateWithoutSupervisorIdRejected() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, null, Map.of()))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("SUPERVISOR_PROFILE_NOT_FOUND"));
  }

  @Test
  void adminCreateWithoutSupervisorIdDerivesFromInternship() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, null, Map.of("internshipId", INTERNSHIP_ID.toString())))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.supervisorId").value(SUPERVISOR_ID.toString()));
  }

  @Test
  void rejectsOutOfRangeScore() throws Exception {
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, 6, Map.of()))))
        .andExpect(status().isBadRequest());
  }

  @Test
  void supervisorUpdatesOwnEvaluation() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(
            put("/evaluations/{id}", id)
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, 5, Map.of("status", "SUBMITTED")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.overallScore").value(92))
        .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
  }

  @Test
  void supervisorCannotUpdateOthersEvaluation() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(
            put("/evaluations/{id}", id)
                .with(auth(OTHER_SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, 5, Map.of()))))
        .andExpect(status().isForbidden());
  }

  @Test
  void cannotRevertSubmittedEvaluationToDraft() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(
            put("/evaluations/{id}", id)
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(request(STUDENT_ID, 5, Map.of("status", "DRAFT")))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
  }

  @Test
  void adminUpdatesAnyEvaluation() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(
            put("/evaluations/{id}", id)
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        request(
                            STUDENT_ID,
                            2,
                            Map.of("supervisorId", SUPERVISOR_ID.toString(),
                                "improvement", "Focus")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.overallScore").value(80))
        .andExpect(jsonPath("$.data.improvement").value("Focus"));
  }

  @Test
  void supervisorListsOwnEvaluations() throws Exception {
    createEvaluation();
    mockMvc
        .perform(get("/evaluations").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void studentListsOwnEvaluationsOnly() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(get("/evaluations").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(id.toString()));
  }

  @Test
  void studentDoesNotSeeDraftEvaluations() throws Exception {
    createEvaluation();
    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    body(
                        request(
                            STUDENT_ID,
                            2,
                            Map.of(
                                "internshipId", INTERNSHIP_ID.toString(),
                                "status", "DRAFT")))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DRAFT"));

    mockMvc
        .perform(get("/evaluations").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void otherStudentSeesNoEvaluations() throws Exception {
    createEvaluation();
    mockMvc
        .perform(get("/evaluations").with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void adminListsAllEvaluations() throws Exception {
    createEvaluation();
    mockMvc
        .perform(get("/evaluations").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void adminDeletesEvaluation() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(delete("/evaluations/{id}", id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(evaluationRepository.findById(id)).isEmpty();
  }

  @Test
  void supervisorCannotDeleteEvaluation() throws Exception {
    UUID id = createEvaluation();
    mockMvc
        .perform(delete("/evaluations/{id}", id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void unknownEvaluationReturns404() throws Exception {
    mockMvc
        .perform(get("/evaluations/{id}", UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("EVALUATION_NOT_FOUND"));
  }

  @Test
  void supervisorCreatesEvaluationWithPaperCriteria() throws Exception {
    Map<String, Object> payload = request(STUDENT_ID, 5, new LinkedHashMap<>());
    payload.put("internshipId", INTERNSHIP_ID.toString());
    payload.put("punctuality", 5);
    payload.put("reliability", 5);
    payload.put("independenceWork", 5);
    payload.put("communicationSkills", 5);
    payload.put("professionalConduct", 5);
    payload.put("learningSpeed", 5);
    payload.put("commitment", 5);
    payload.put("accuracy", 5);
    payload.put("teamContribution", 5);
    payload.put("cooperation", 5);
    payload.put("technicalProficiency", 5);
    payload.put("organizationalSkills", 5);
    payload.put("projectSupport", 5);
    payload.put("taskResponsibility", 15);
    payload.put("qualityTeamMember", 20);
    payload.put("remarks", "Excellent work");
    payload.put("department", "CS");
    payload.put("companyName", "Acme Corp");

    mockMvc
        .perform(
            post("/evaluations")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(payload)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.overallScore").value(100))
        .andExpect(jsonPath("$.data.department").value("CS"))
        .andExpect(jsonPath("$.data.companyName").value("Acme Corp"))
        .andExpect(jsonPath("$.data.punctuality").value(5))
        .andExpect(jsonPath("$.data.taskResponsibility").value(15))
        .andExpect(jsonPath("$.data.qualityTeamMember").value(20))
        .andExpect(jsonPath("$.data.remarks").value("Excellent work"));
  }

  @Test
  void facultyCanListEvaluations() throws Exception {
    mockMvc
        .perform(get("/evaluations").with(auth(FACULTY_APP_USER.toString(), "FACULTY")))
        .andExpect(status().isOk());
  }

  @Test
  void facultySeesOwnAssignedStudentsInAssignees() throws Exception {
    mockMvc
        .perform(get("/evaluations/assignees").with(auth(FACULTY_APP_USER.toString(), "FACULTY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student One"));
  }

  @Test
  void facultyCanViewAnotherAcademicStaffMembersAssigneesWithPicker() throws Exception {
    mockMvc
        .perform(
            get("/evaluations/assignees")
                .param("academicStaffId", OTHER_FACULTY_ID.toString())
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].fullName").value("Student Two"));
  }

  @Test
  void facultyReadsEvaluationThroughAcademicStaffPickerOnly() throws Exception {
    String json =
        mockMvc
            .perform(
                post("/evaluations")
                    .with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        body(
                            request(
                                OTHER_STUDENT_ID,
                                4,
                                Map.of("supervisorId", OTHER_SUPERVISOR_ID.toString())))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(json).path("data").path("id").asText());

    mockMvc
        .perform(
            get("/evaluations/{id}", id)
                .param("academicStaffId", OTHER_FACULTY_ID.toString())
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(OTHER_STUDENT_ID.toString()));

    mockMvc
        .perform(get("/evaluations/{id}", id).with(auth(FACULTY_APP_USER.toString(), "FACULTY")))
        .andExpect(status().isForbidden());
  }
}