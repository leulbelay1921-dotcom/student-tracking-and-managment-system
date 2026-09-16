package com.sitms.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.jdbc.core.JdbcTemplate;

/** Integration tests for {@link StudentController} (SITMS-007): RBAC, validation, CRUD. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StudentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private StudentRepository studentRepository;
  @Autowired private AppUserIdentity appUserIdentity;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;
  @Autowired private JdbcTemplate jdbcTemplate;

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

  private Map<String, Object> payload(String appUserUsername, String studentNo) {
    return Map.of(
        "appUserUsername", appUserUsername,
        "studentNo", studentNo,
        "fullName", "Alice Smith",
        "program", "CS",
        "level", 1);
  }

  private UUID appUserIdOf(String createdBody) throws Exception {
    return UUID.fromString(
        objectMapper.readTree(createdBody).path("data").path("appUserId").asText());
  }

  @Test
  void anonymousCannotListStudents() throws Exception {
    mockMvc.perform(get("/students")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanListStudents() throws Exception {
    // Seed the student so the STUDENT scope resolves to a single profile.
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-list", "S-LST"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(created);

    mockMvc
        .perform(get("/students").with(auth(sub.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.items.length()").value(1));
  }

  @Test
  void studentListIsScopedToOwnProfile() throws Exception {
    String subCreated =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-scope", "S-SCOPE"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(subCreated);
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-other", "S-OTHER"))))
        .andExpect(status().isOk());

    // STUDENT list returns only the caller's own profile, not the other student.
    mockMvc
        .perform(get("/students").with(auth(sub.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].studentNo").value("S-SCOPE"));
  }

  @Test
  void studentCannotCreateStudent() throws Exception {
    String body =
        objectMapper.writeValueAsString(payload("fk-forbid", "S-001"));
    mockMvc
        .perform(
            post("/students").with(auth("stu", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorCannotCreateStudent() throws Exception {
    String body = objectMapper.writeValueAsString(payload("fk-forbid2", "S-002"));
    mockMvc
        .perform(
            post("/students").with(auth("sup", "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesStudentCreatingAppUser() throws Exception {
    String body = objectMapper.writeValueAsString(payload("fk-100", "S-100"));
    String response =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.studentNo").value("S-100"))
            .andExpect(jsonPath("$.data.appUserUsername").value("fk-100"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
    UUID appUserId = appUserIdOf(response);
    assertThat(studentRepository.findById(id)).isPresent();
    assertThat(studentRepository.findByAppUserId(appUserId)).isPresent();
    assertThat(appUserIdentity.existsByUsername("fk-100")).isTrue();
  }

  @Test
  void duplicateStudentNoReturnsConflict() throws Exception {
    String first = objectMapper.writeValueAsString(payload("fk-200", "S-200"));
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(first))
        .andExpect(status().isOk());

    String second = objectMapper.writeValueAsString(payload("fk-201", "S-200"));
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(second))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(StudentService.DUPLICATE_STUDENT_NO));
  }

  @Test
  void duplicateAppUserUsernameReturnsConflict() throws Exception {
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-dup", "S-210"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-dup", "S-211"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(StudentService.DUPLICATE_USERNAME));
  }

  @Test
  void validationRejectsBlankFields() throws Exception {
    String body = "{\"appUserUsername\":\"\",\"studentNo\":\"\",\"fullName\":\"\",\"program\":\"\",\"level\":0}";
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminUpdatesAndDeletesStudent() throws Exception {
    String createBody = objectMapper.writeValueAsString(payload("fk-300", "S-300"));
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createBody))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    String updateBody = objectMapper.writeValueAsString(payload("fk-300", "S-301"));
    mockMvc
        .perform(
            put("/students/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentNo").value("S-301"));

    mockMvc
        .perform(delete("/students/" + id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(studentRepository.findById(id)).isEmpty();
  }

  @Test
  void getMissingStudentReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/students/" + UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void studentCanAccessOwnProfile() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-500", "S-500"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(created);

    mockMvc
        .perform(get("/students/me").with(auth(sub.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentNo").value("S-500"))
        .andExpect(jsonPath("$.data.appUserId").value(sub.toString()));
  }

  @Test
  void studentCannotAccessAnotherStudentsProfile() throws Exception {
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-600", "S-600"))))
        .andExpect(status().isOk());

    // Different authenticated user who has no profile -> 404 (own-data only).
    mockMvc
        .perform(get("/students/me").with(auth("22222222-2222-2222-2222-222222222222", "STUDENT")))
        .andExpect(status().isNotFound());
  }

  @Test
  void studentCanUpdateOwnContactInfo() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-700", "S-700"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(created);

    String body = "{\"email\":\"alice@example.com\",\"phone\":\"+1 555 0100\"}";
    mockMvc
        .perform(
            put("/students/me").with(auth(sub.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.email").value("alice@example.com"))
        .andExpect(jsonPath("$.data.phone").value("+1 555 0100"))
        .andExpect(jsonPath("$.data.studentNo").value("S-700"))
        .andExpect(jsonPath("$.data.fullName").value("Alice Smith"));
  }

  @Test
  void studentCannotChangeOfficialFieldsThroughOwnProfileEndpoint() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-701", "S-701"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(created);

    // A full StudentRequest-shaped payload is ignored for official fields; only email/phone apply.
    String body =
        "{\"appUserUsername\":\"fk-701\",\"studentNo\":\"S-999\",\"fullName\":\"Changed\","
            + "\"program\":\"PHYSICS\",\"level\":9,\"university\":\"Hacked U\",\"phone\":\"123\"}";
    mockMvc
        .perform(
            put("/students/me").with(auth(sub.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentNo").value("S-701"))
        .andExpect(jsonPath("$.data.fullName").value("Alice Smith"))
        .andExpect(jsonPath("$.data.program").value("CS"))
        .andExpect(jsonPath("$.data.level").value(1))
        .andExpect(jsonPath("$.data.university").value(org.hamcrest.Matchers.emptyOrNullString()))
        .andExpect(jsonPath("$.data.phone").value("123"));
  }

  @Test
  void studentCanUploadProfilePhoto() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-710", "S-710"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(created);

    mockMvc
        .perform(
            multipart("/students/me/photo")
                .file(new MockMultipartFile("file", "photo.png", "image/png", new byte[] {1, 2, 3}))
                .with(auth(sub.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.profilePhoto").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
  }

  @Test
  void adminCanUploadProfilePhotoForStudent() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-711", "S-711"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            multipart("/students/" + id + "/photo")
                .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[] {9, 9}))
                .with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.profilePhoto").value(org.hamcrest.Matchers.startsWith("data:image/jpeg;base64,")));
  }

  @Test
  void nonImagePhotoIsRejected() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-712", "S-712"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID sub = appUserIdOf(created);

    mockMvc
        .perform(
            multipart("/students/me/photo")
                .file(new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[] {1}))
                .with(auth(sub.toString(), "STUDENT")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(StudentService.INVALID_PHOTO_TYPE));
  }

  @Test
  void adminCannotUseMeEndpoint() throws Exception {
    mockMvc
        .perform(get("/students/me").with(auth("admin", "ADMIN")))
        .andExpect(status().isForbidden());
  }

  @Test
  void createStoresDepartmentAndStatus() throws Exception {
    String body =
        "{\"appUserUsername\":\"fk-dpt\",\"studentNo\":\"S-DPT\",\"fullName\":\"Dept Student\","
            + "\"program\":\"CS\",\"department\":\"Engineering\",\"status\":\"ACTIVE\",\"level\":2}";
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.department").value("Engineering"))
        .andExpect(jsonPath("$.data.status").value("ACTIVE"));
  }

  @Test
  void createDefaultsStatusToPendingWhenOmitted() throws Exception {
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-def", "S-DEF"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.department").value(org.hamcrest.Matchers.nullValue()));
  }

  @Test
  void updateStoresDepartmentAndStatus() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-upd", "S-UPD"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    String updateBody =
        "{\"appUserUsername\":\"fk-upd\",\"studentNo\":\"S-UPD\",\"fullName\":\"Updated\","
            + "\"program\":\"CS\",\"department\":\"Business\",\"status\":\"COMPLETED\",\"level\":3}";
    mockMvc
        .perform(
            put("/students/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.department").value("Business"))
        .andExpect(jsonPath("$.data.status").value("COMPLETED"))
        .andExpect(jsonPath("$.data.level").value(3));
  }

  @Test
  void adminCanRenameAppUserUsername() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-ren", "S-REN"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            put("/students/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-ren2", "S-REN"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appUserUsername").value("fk-ren2"));

    assertThat(appUserIdentity.existsByUsername("fk-ren2")).isTrue();
    assertThat(appUserIdentity.existsByUsername("fk-ren")).isFalse();
  }

  @Test
  void adminCannotRenameToTakenAppUserUsername() throws Exception {
    String body = objectMapper.writeValueAsString(payload("fk-taken", "S-TKEN"));
    mockMvc
        .perform(
            post("/students").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk());
    String created =
        mockMvc
            .perform(
                post("/students").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("fk-own", "S-TOWN"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            put("/students/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("fk-taken", "S-TOWN"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(StudentService.DUPLICATE_USERNAME));
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

  @Test
  void supervisorListsOnlyTheirOwnStudentsByDefault() throws Exception {
    UUID stuApp = UUID.randomUUID();
    UUID stuId = UUID.randomUUID();
    UUID supApp = UUID.randomUUID();
    UUID supId = UUID.randomUUID();
    UUID orgId = UUID.randomUUID();
    UUID otherStuApp = UUID.randomUUID();
    UUID otherStuId = UUID.randomUUID();
    UUID otherSupApp = UUID.randomUUID();
    UUID otherSupId = UUID.randomUUID();

    seedAppUser(stuApp, "stu-assigned", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-A1', 'Student Alpha', 'CS', ?)",
        stuId,
        stuApp,
        stuApp);
    seedAppUser(otherStuApp, "stu-other", "STUDENT");
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-B1', 'Student Beta', 'CS', ?)",
        otherStuId,
        otherStuApp,
        otherStuApp);
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Org', ?)",
        orgId,
        stuApp);
    seedAppUser(supApp, "sup-a", "SUPERVISOR");
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'E-A', 'Sup Alpha', ?)",
        supId,
        supApp,
        orgId,
        supApp);
    seedAppUser(otherSupApp, "sup-b", "SUPERVISOR");
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'E-B', 'Sup Beta', ?)",
        otherSupId,
        otherSupApp,
        orgId,
        otherSupApp);

    UUID internshipId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, created_by)"
            + " VALUES (?, ?, ?, ?, 'IA', 'APPROVED', ?)",
        internshipId,
        stuId,
        orgId,
        supId,
        supApp);
    UUID otherInternshipId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, created_by)"
            + " VALUES (?, ?, ?, ?, 'IB', 'APPROVED', ?)",
        otherInternshipId,
        otherStuId,
        orgId,
        otherSupId,
        otherSupApp);

    // Own list is still scoped to the calling supervisor.
    mockMvc
        .perform(get("/students").with(auth(supApp.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].studentNo").value("S-A1"));

    // Passing supervisorId lists that supervisor's assigned students instead.
    mockMvc
        .perform(
            get("/students")
                .with(auth(supApp.toString(), "SUPERVISOR"))
                .param("supervisorId", otherSupId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1))
        .andExpect(jsonPath("$.data.items[0].studentNo").value("S-B1"));
  }
}
