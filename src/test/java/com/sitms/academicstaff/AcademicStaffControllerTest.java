package com.sitms.academicstaff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitms.student.AppUserIdentity;
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
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link AcademicStaffController} (SITMS-072): RBAC, validation, CRUD. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AcademicStaffControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private AcademicStaffRepository academicStaffRepository;
  @Autowired private AppUserIdentity appUserIdentity;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void setUp() {}

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

  private Map<String, Object> payload(String staffNo, String username) {
    return Map.of(
        "appUserUsername", username,
        "staffNo", staffNo,
        "fullName", "Jane Doe",
        "title", "Senior Lecturer",
        "department", "Software Engineering");
  }

  private Map<String, Object> payload(String staffNo) {
    return payload(staffNo, "fac-" + staffNo.toLowerCase());
  }

  /** Creates a member via the ADMIN endpoint; the identity is auto-created from the id. */
  private void createMember(String staffNo, String department) {
    Map<String, Object> p = new LinkedHashMap<>(payload(staffNo));
    p.put("department", department);
    try {
      mockMvc
          .perform(
              post("/academic-staff").with(auth("admin", "ADMIN"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(p)))
          .andExpect(status().isOk());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void anonymousCannotList() throws Exception {
    mockMvc.perform(get("/academic-staff")).andExpect(status().isUnauthorized());
  }

  @Test
  void anyAuthenticatedRoleCanRead() throws Exception {
    mockMvc.perform(get("/academic-staff").with(auth("admin", "ADMIN"))).andExpect(status().isOk());
    mockMvc
        .perform(get("/academic-staff").with(auth("fac", "FACULTY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
    mockMvc.perform(get("/academic-staff").with(auth("stu", "STUDENT"))).andExpect(status().isOk());
  }

  @Test
  void studentCannotCreate() throws Exception {
    mockMvc
        .perform(
            post("/academic-staff").with(auth("stu", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-001"))))
        .andExpect(status().isForbidden());
    mockMvc
        .perform(
            post("/academic-staff").with(auth("fac", "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-001"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesAcademicStaff() throws Exception {
    String username = "fac-jane";
    String response =
        mockMvc
            .perform(
                post("/academic-staff").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("FAC-002", username))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.staffNo").value("FAC-002"))
            .andExpect(jsonPath("$.data.fullName").value("Jane Doe"))
            .andExpect(jsonPath("$.data.appUserUsername").value(username))
            .andExpect(jsonPath("$.data.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
    assertThat(academicStaffRepository.findById(id)).isPresent();
    assertThat(appUserIdentity.existsByUsername(username)).isTrue();
  }

  @Test
  void createReusesExistingAppUserIdentity() throws Exception {
    String username = "fac-reuse";
    appUserIdentity.ensureRole(username, "Jane Doe", "FACULTY");
    String response =
        mockMvc
            .perform(
                post("/academic-staff").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("FAC-REU", username))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appUserUsername").value(username))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(appUserIdentity.existsByUsername(username)).isTrue();
  }

  @Test
  void duplicateStaffNoReturnsConflict() throws Exception {
    mockMvc
        .perform(
            post("/academic-staff").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-200"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/academic-staff").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-200"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(AcademicStaffService.DUPLICATE_STAFF_NO));
  }

  @Test
  void duplicateAppUserUsernameReturnsConflict() throws Exception {
    String username = "fac-dup";
    mockMvc
        .perform(
            post("/academic-staff").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-210", username))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/academic-staff").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-211", username))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(AcademicStaffService.DUPLICATE_ACADEMIC_STAFF));
  }

  @Test
  void validationRejectsInvalidAppUserUsername() throws Exception {
    String body = "{\"appUserUsername\":\"ab\",\"staffNo\":\"FAC-300\",\"fullName\":\"X\"}";
    mockMvc
        .perform(
            post("/academic-staff").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminUpdatesAndDeletes() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/academic-staff").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("FAC-400"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    Map<String, Object> update = new LinkedHashMap<>(payload("FAC-400"));
    update.put("active", false);
    mockMvc
        .perform(
            put("/academic-staff/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.fullName").value("Jane Doe"))
        .andExpect(jsonPath("$.data.active").value(false));

    mockMvc.perform(delete("/academic-staff/" + id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(academicStaffRepository.findById(id)).isEmpty();
  }

  @Test
  void adminCanRenameAppUserUsername() throws Exception {
    String createdAt =
        mockMvc
            .perform(
                post("/academic-staff").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("FAC-450", "fac-old"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(createdAt).path("data").path("id").asText());

    mockMvc
        .perform(
            put("/academic-staff/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("FAC-450", "fac-new"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appUserUsername").value("fac-new"));

    assertThat(appUserIdentity.existsByUsername("fac-new")).isTrue();
    assertThat(appUserIdentity.existsByUsername("fac-old")).isFalse();
  }

  @Test
  void memberResolvesOwnProfileViaMe() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/academic-staff").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("FAC-900", "fac-me"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID appUserId =
        UUID.fromString(objectMapper.readTree(created).path("data").path("appUserId").asText());

    mockMvc
        .perform(get("/academic-staff/me").with(auth(appUserId.toString(), "FACULTY")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.staffNo").value("FAC-900"))
        .andExpect(jsonPath("$.data.fullName").value("Jane Doe"));
  }

  @Test
  void userWithoutProfileReturnsNotFound() throws Exception {
    UUID noProfile = UUID.randomUUID();
    mockMvc
        .perform(get("/academic-staff/me").with(auth(noProfile.toString(), "FACULTY")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listFiltersByDepartmentCaseInsensitive() throws Exception {
    createMember("FAC-500", "Software Engineering");
    createMember("FAC-501", "Data Science");

    mockMvc
        .perform(
            get("/academic-staff")
                .param("department", "software engineering")
                .with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].department").value("Software Engineering"));

    mockMvc
        .perform(
            get("/academic-staff").param("department", "unknown").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void listsDistinctDepartments() throws Exception {
    createMember("FAC-600", "Software Engineering");
    createMember("FAC-601", "Data Science");
    createMember("FAC-602", "Software Engineering");

    mockMvc
        .perform(get("/academic-staff/departments").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items.length()").value(2));
  }
}