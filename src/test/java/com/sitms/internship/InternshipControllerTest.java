package com.sitms.internship;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
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

/** Integration tests for {@link InternshipController} (SITMS-019): RBAC, CRUD, status lifecycle. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class InternshipControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID FACULTY_APP_USER = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID FACULTY_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("99999999-9999-9999-9999-999999999991");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999992");
  private static final UUID OTHER_SUPERVISOR_APP_USER = UUID.fromString("99999999-9999-9999-9999-999999999993");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("99999999-9999-9999-9999-999999999994");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private InternshipRepository internshipRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;
  @Autowired private EntityManager entityManager;

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

  private void seedOtherParents() {
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
  }

  private UUID createInternshipFor(UUID studentId, UUID supervisorId) throws Exception {
    Map<String, Object> p = new java.util.LinkedHashMap<>(payload());
    p.put("studentId", studentId.toString());
    if (supervisorId == null) {
      p.remove("supervisorId");
    } else {
      p.put("supervisorId", supervisorId.toString());
    }
    String response =
        mockMvc
            .perform(
                post("/internships")
                    .with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(p)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
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
        "studentId", STUDENT_ID.toString(),
        "organizationId", ORGANIZATION_ID.toString(),
        "supervisorId", SUPERVISOR_ID.toString(),
        "title", "Software Engineering Internship",
        "description", "Frontend work");
  }

  @Test
  void anonymousCannotListInternships() throws Exception {
    mockMvc.perform(get("/internships")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanListInternships() throws Exception {
    mockMvc
        .perform(get("/internships").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void studentCannotCreateInternship() throws Exception {
    mockMvc
        .perform(
            post("/internships").with(auth("stu", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesInternshipAsPending() throws Exception {
    String response =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.title").value("Software Engineering Internship"))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
    assertThat(internshipRepository.findById(id)).isPresent();
  }

  @Test
  void studentCannotChangeStatus() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/internships/" + id + "/status").with(auth("stu", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorApprovesInternship() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/internships/" + id + "/status").with(auth("sup", "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("APPROVED"));
  }

  @Test
  void invalidStatusTransitionReturnsConflict() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    // PENDING -> COMPLETED is not a valid transition.
    mockMvc
        .perform(
            patch("/internships/" + id + "/status").with(auth("sup", "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(InternshipService.INVALID_STATUS_TRANSITION));
  }

  @Test
  void validationRejectsMissingStudentId() throws Exception {
    String body = "{\"organizationId\":\"" + ORGANIZATION_ID + "\",\"title\":\"T\"}";
    mockMvc
        .perform(
            post("/internships").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminUpdatesAndDeletesInternship() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    Map<String, Object> update = new java.util.LinkedHashMap<>(payload());
    update.put("title", "Updated Internship");
    mockMvc
        .perform(
            put("/internships/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Updated Internship"));

    mockMvc
        .perform(delete("/internships/" + id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(internshipRepository.findById(id)).isEmpty();
  }

  @Test
  void getMissingInternshipReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/internships/" + UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void studentSeesOnlyOwnInternships() throws Exception {
    seedOtherParents();
    createInternshipFor(STUDENT_ID, null);
    createInternshipFor(OTHER_STUDENT_ID, null);

    mockMvc
        .perform(get("/internships").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void studentCannotGetAnotherStudentsInternship() throws Exception {
    seedOtherParents();
    UUID otherId = createInternshipFor(OTHER_STUDENT_ID, null);

    mockMvc
        .perform(get("/internships/" + otherId).with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(InternshipService.INTERNSHIP_NOT_FOUND));
  }

  @Test
  void studentGetsOwnInternshipMeWithResolvedNames() throws Exception {
    UUID id =
        internshipRepository
            .saveAndFlush(
                Internship.builder()
                    .studentId(STUDENT_ID)
                    .organizationId(ORGANIZATION_ID)
                    .supervisorId(SUPERVISOR_ID)
                    .academicStaffId(FACULTY_ID)
                    .title("Software Developer")
                    .description("Backend work")
                    .startDate(LocalDate.of(2026, 7, 8))
                    .endDate(LocalDate.of(2026, 9, 8))
                    .status(InternshipStatus.APPROVED)
                    .build())
            .getId();

    mockMvc
        .perform(get("/internships/me").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(id.toString()))
        .andExpect(jsonPath("$.data.organizationName").value("Acme Corp"))
        .andExpect(jsonPath("$.data.supervisorName").value("Sup One"))
        .andExpect(jsonPath("$.data.academicStaffName").value("Prof One"))
        .andExpect(jsonPath("$.data.title").value("Software Developer"))
        .andExpect(jsonPath("$.data.startDate").value("2026-07-08"))
        .andExpect(jsonPath("$.data.endDate").value("2026-09-08"))
        .andExpect(jsonPath("$.data.status").value("APPROVED"));
  }

  @Test
  void studentMeWithoutInternshipReturnsNullData() throws Exception {
    mockMvc
        .perform(get("/internships/me").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(nullValue()));
  }

  @Test
  void studentMeIgnoresAnotherStudentsInternship() throws Exception {
    seedOtherParents();
    internshipRepository.saveAndFlush(
        Internship.builder()
            .studentId(OTHER_STUDENT_ID)
            .organizationId(ORGANIZATION_ID)
            .supervisorId(OTHER_SUPERVISOR_ID)
            .title("Other Internship")
            .build());

    mockMvc
        .perform(get("/internships/me").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(nullValue()));
  }

  @Test
  void studentWithNoLinkedProfileCannotReadMe() throws Exception {
    mockMvc
        .perform(
            get("/internships/me").with(auth(UUID.randomUUID().toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_PROFILE_NOT_FOUND"));
  }

  @Test
  void supervisorSeesOnlyAssignedInternships() throws Exception {
    seedOtherParents();
    UUID assignedId = createInternshipFor(STUDENT_ID, SUPERVISOR_ID);
    UUID otherId = createInternshipFor(OTHER_STUDENT_ID, OTHER_SUPERVISOR_ID);

    mockMvc
        .perform(get("/internships").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mockMvc
        .perform(get("/internships/" + assignedId).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/internships/" + otherId).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(InternshipService.INTERNSHIP_NOT_FOUND));
  }

  @Test
  void supervisorCanViewAnotherSupervisorsAssignedInternships() throws Exception {
    seedOtherParents();
    createInternshipFor(STUDENT_ID, SUPERVISOR_ID);
    UUID otherId = createInternshipFor(OTHER_STUDENT_ID, OTHER_SUPERVISOR_ID);

    mockMvc
        .perform(
            get("/internships")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(otherId.toString()));
  }

  @Test
  void supervisorReassignsOwnStudentToAnotherSupervisor() throws Exception {
    seedOtherParents();
    UUID id = createInternshipFor(STUDENT_ID, SUPERVISOR_ID);

    mockMvc
        .perform(
            patch("/internships/" + id + "/supervisors")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supervisorId\":\"" + OTHER_SUPERVISOR_ID + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.supervisorId").value(OTHER_SUPERVISOR_ID.toString()));
  }

  @Test
  void supervisorCannotReassignAnotherSupervisorsStudent() throws Exception {
    seedOtherParents();
    UUID otherId = createInternshipFor(OTHER_STUDENT_ID, OTHER_SUPERVISOR_ID);

    mockMvc
        .perform(
            patch("/internships/" + otherId + "/supervisors")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supervisorId\":\"" + SUPERVISOR_ID + "\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(InternshipService.INTERNSHIP_NOT_FOUND));
  }

  @Test
  void adminAssignsCompanyAndAcademicSupervisors() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/internships/" + id + "/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supervisorId\":\"" + SUPERVISOR_ID + "\",\"academicStaffId\":\""
                    + FACULTY_ID + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.supervisorId").value(SUPERVISOR_ID.toString()))
        .andExpect(jsonPath("$.data.academicStaffId").value(FACULTY_ID.toString()));

    assertThat(internshipRepository.findById(id)).get().extracting(Internship::getAcademicStaffId)
        .isEqualTo(FACULTY_ID);
  }

  @Test
  void supervisorCanBeAssignedToMultipleStudents() throws Exception {
    seedOtherParents();
    createInternshipFor(STUDENT_ID, SUPERVISOR_ID);
    createInternshipFor(OTHER_STUDENT_ID, SUPERVISOR_ID);
  }

  @Test
  void cancelledInternshipWithDifferentSupervisorDoesNotBlockNewAssignment() throws Exception {
    seedOtherParents();
    UUID cancelled = createInternshipFor(STUDENT_ID, OTHER_SUPERVISOR_ID);
    mockMvc
        .perform(
            patch("/internships/" + cancelled + "/status").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            patch("/internships/" + cancelled + "/status").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELLED\"}"))
        .andExpect(status().isOk());

    entityManager.clear();
    createInternshipFor(STUDENT_ID, SUPERVISOR_ID);
  }

  @Test
  void studentCannotBeAssignedTwoDifferentCompanySupervisors() throws Exception {
    seedOtherParents();
    createInternshipFor(STUDENT_ID, SUPERVISOR_ID);

    Map<String, Object> second = new java.util.LinkedHashMap<>(payload());
    second.put("supervisorId", OTHER_SUPERVISOR_ID.toString());
    mockMvc
        .perform(
            post("/internships").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errorCode")
                .value(InternshipService.STUDENT_ALREADY_ASSIGNED_COMPANY_SUPERVISOR));
  }

  @Test
  void assigningDifferentCompanySupervisorViaPatchIsRejected() throws Exception {
    seedOtherParents();
    createInternshipFor(STUDENT_ID, SUPERVISOR_ID);

    Map<String, Object> second = new java.util.LinkedHashMap<>(payload());
    second.remove("supervisorId");
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(second)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID secondId = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/internships/" + secondId + "/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supervisorId\":\"" + OTHER_SUPERVISOR_ID + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errorCode")
                .value(InternshipService.STUDENT_ALREADY_ASSIGNED_COMPANY_SUPERVISOR));
  }

  @Test
  void assigningSameCompanySupervisorToSecondInternshipIsAllowed() throws Exception {
    createInternshipFor(STUDENT_ID, SUPERVISOR_ID);

    Map<String, Object> second = new java.util.LinkedHashMap<>(payload());
    second.remove("supervisorId");
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(second)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID secondId = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            patch("/internships/" + secondId + "/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"supervisorId\":\"" + SUPERVISOR_ID + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.supervisorId").value(SUPERVISOR_ID.toString()));
  }

  @Test
  void studentCannotBeAssignedTwoDifferentUniversitySupervisors() throws Exception {
    UUID otherFacultyAppUser = UUID.randomUUID();
    UUID otherFacultyId = UUID.randomUUID();
    seedAppUser(otherFacultyAppUser, "faculty3", "FACULTY");
    jdbcTemplate.update(
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, created_by) "
            + "VALUES (?, ?, 'FAC-3', 'Prof Three', ?)",
        otherFacultyId,
        otherFacultyAppUser,
        otherFacultyAppUser);

    Map<String, Object> first = new java.util.LinkedHashMap<>(payload());
    first.put("academicStaffId", FACULTY_ID.toString());
    mockMvc
        .perform(
            post("/internships").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(first)))
        .andExpect(status().isOk());

    Map<String, Object> second = new java.util.LinkedHashMap<>(payload());
    second.remove("supervisorId");
    second.put("academicStaffId", otherFacultyId.toString());
    mockMvc
        .perform(
            post("/internships").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(second)))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errorCode")
                .value(InternshipService.STUDENT_ALREADY_ASSIGNED_UNIVERSITY_SUPERVISOR));
  }

  @Test
  void completingWithoutSubmittedEvaluationIsRejected() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/internships").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload())))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());
    mockMvc
        .perform(
            patch("/internships/" + id + "/status").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/internships/" + id + "/status").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(InternshipService.EVALUATION_REQUIRED_TO_COMPLETE));
  }

  @Test
  void completingWithSubmittedEvaluationSucceeds() throws Exception {
    UUID id =
        internshipRepository
            .saveAndFlush(
                Internship.builder()
                    .studentId(STUDENT_ID)
                    .organizationId(ORGANIZATION_ID)
                    .supervisorId(SUPERVISOR_ID)
                    .title("Software Engineering Internship")
                    .description("Frontend work")
                    .status(InternshipStatus.APPROVED)
                    .build())
            .getId();

    mockMvc
        .perform(
            post("/evaluations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"studentId\":\"" + STUDENT_ID + "\",\"internshipId\":\"" + id
                    + "\",\"supervisorId\":\"" + SUPERVISOR_ID
                    + "\",\"technicalSkills\":4,\"communication\":4,\"teamwork\":4,"
                    + "\"problemSolving\":4,\"professionalism\":4}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            patch("/internships/" + id + "/status").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));
  }
}
