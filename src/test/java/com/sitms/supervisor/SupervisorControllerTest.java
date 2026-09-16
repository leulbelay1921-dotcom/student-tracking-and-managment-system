package com.sitms.supervisor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sitms.organization.Organization;
import com.sitms.organization.OrganizationRepository;
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

/** Integration tests for {@link SupervisorController} (SITMS-015): RBAC, validation, CRUD. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class SupervisorControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SupervisorRepository supervisorRepository;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private AppUserIdentity appUserIdentity;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private UUID organizationId;

  @BeforeEach
  void seedOrganization() {
    Organization org = organizationRepository.save(Organization.builder().name("Acme Corp").build());
    organizationId = org.getId();
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

  private Map<String, Object> payload(String employeeNo, String username) {
    return Map.of(
        "appUserUsername", username,
        "organizationId", organizationId.toString(),
        "employeeNo", employeeNo,
        "fullName", "Jane Doe",
        "jobTitle", "Engineering Lead",
        "department", "R&D");
  }

  private Map<String, Object> payload(String employeeNo) {
    return payload(employeeNo, "sup-" + employeeNo.toLowerCase());
  }

  /** Creates a supervisor via the ADMIN endpoint; the identity is auto-created from the id. */
  private void createSupervisor(String employeeNo, String department) {
    Map<String, Object> p = new LinkedHashMap<>(payload(employeeNo));
    p.put("department", department);
    try {
      mockMvc
          .perform(
              post("/supervisors").with(auth("admin", "ADMIN"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(p)))
          .andExpect(status().isOk());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void anonymousCannotListSupervisors() throws Exception {
    mockMvc.perform(get("/supervisors")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanListSupervisors() throws Exception {
    mockMvc
        .perform(get("/supervisors").with(auth("stu", "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void studentCannotCreateSupervisor() throws Exception {
    mockMvc
        .perform(
            post("/supervisors").with(auth("stu", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("EMP-001"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesSupervisor() throws Exception {
    String username = "sup-jane";
    String response =
        mockMvc
            .perform(
                post("/supervisors").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("EMP-001", username))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.employeeNo").value("EMP-001"))
            .andExpect(jsonPath("$.data.fullName").value("Jane Doe"))
            .andExpect(jsonPath("$.data.appUserUsername").value(username))
            .andExpect(jsonPath("$.data.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
    assertThat(supervisorRepository.findById(id)).isPresent();
    assertThat(appUserIdentity.existsByUsername(username)).isTrue();
  }

  @Test
  void createReusesExistingAppUserIdentity() throws Exception {
    String username = "sup-reuse";
    appUserIdentity.ensureRole(username, "Jane Doe", "SUPERVISOR");
    String response =
        mockMvc
            .perform(
                post("/supervisors").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("EMP-REU", username))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.appUserUsername").value(username))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(appUserIdentity.existsByUsername(username)).isTrue();
  }

  @Test
  void duplicateEmployeeNoReturnsConflict() throws Exception {
    mockMvc
        .perform(
            post("/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("EMP-200"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("EMP-200"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(SupervisorService.DUPLICATE_EMPLOYEE_NO));
  }

  @Test
  void duplicateAppUserUsernameReturnsConflict() throws Exception {
    String username = "sup-dup";
    mockMvc
        .perform(
            post("/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("EMP-210", username))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("EMP-211", username))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(SupervisorService.DUPLICATE_SUPERVISOR));
  }

  @Test
  void validationRejectsMissingOrganizationId() throws Exception {
    String body = "{\"appUserUsername\":\"sup-norg\",\"employeeNo\":\"EMP-300\",\"fullName\":\"X\"}";
    mockMvc
        .perform(
            post("/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void validationRejectsInvalidAppUserUsername() throws Exception {
    String body =
        "{\"appUserUsername\":\"ab\",\"organizationId\":\"" + organizationId
            + "\",\"employeeNo\":\"EMP-301\",\"fullName\":\"X\"}";
    mockMvc
        .perform(
            post("/supervisors").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminUpdatesAndDeletesSupervisor() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/supervisors").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("EMP-400"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    Map<String, Object> update = new LinkedHashMap<>(payload("EMP-400"));
    update.put("active", false);
    mockMvc
        .perform(
            put("/supervisors/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.active").value(false));

    mockMvc
        .perform(delete("/supervisors/" + id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(supervisorRepository.findById(id)).isEmpty();
  }

  @Test
  void adminCanRenameAppUserUsername() throws Exception {
    String createdAt =
        mockMvc
            .perform(
                post("/supervisors").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("EMP-450", "sup-old"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(createdAt).path("data").path("id").asText());

    mockMvc
        .perform(
            put("/supervisors/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("EMP-450", "sup-new"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.appUserUsername").value("sup-new"));

    assertThat(appUserIdentity.existsByUsername("sup-new")).isTrue();
    assertThat(appUserIdentity.existsByUsername("sup-old")).isFalse();
  }

  @Test
  void getMissingSupervisorReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/supervisors/" + UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void listFiltersByDepartment() throws Exception {
    createSupervisor("EMP-500", "R&D");
    createSupervisor("EMP-501", "Sales");

    mockMvc
        .perform(get("/supervisors").param("department", "R&D").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].department").value("R&D"));

    mockMvc
        .perform(get("/supervisors").param("department", "unknown").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(0));
  }

  @Test
  void listFiltersByDepartmentCaseInsensitive() throws Exception {
    createSupervisor("EMP-600", "Engineering");

    mockMvc
        .perform(get("/supervisors").param("department", "engineering").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void listsDistinctDepartments() throws Exception {
    createSupervisor("EMP-700", "R&D");
    createSupervisor("EMP-701", "Sales");
    createSupervisor("EMP-702", "R&D");

    mockMvc
        .perform(get("/supervisors/departments").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items.length()").value(2));
  }

  @Test
  void supervisorResolvesOwnProfileViaMe() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/supervisors").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("EMP-900", "sup-me"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID appUserId =
        UUID.fromString(objectMapper.readTree(created).path("data").path("appUserId").asText());

    mockMvc
        .perform(get("/supervisors/me").with(auth(appUserId.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.employeeNo").value("EMP-900"))
        .andExpect(jsonPath("$.data.fullName").value("Jane Doe"));
  }

  @Test
  void supervisorWithoutProfileReturnsNotFound() throws Exception {
    UUID noProfile = UUID.randomUUID();
    mockMvc
        .perform(get("/supervisors/me").with(auth(noProfile.toString(), "SUPERVISOR")))
        .andExpect(status().isNotFound());
  }
}