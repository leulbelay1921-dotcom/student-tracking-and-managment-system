package com.sitms.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/** Integration tests for {@link OrganizationController} (SITMS-012): RBAC, validation, CRUD. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrganizationControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

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

  private Map<String, Object> payload(String name) {
    return Map.of(
        "name", name,
        "industry", "Technology",
        "contactEmail", "hr@" + name.toLowerCase() + ".com",
        "website", "https://" + name.toLowerCase() + ".com");
  }

  @Test
  void anonymousCannotListOrganizations() throws Exception {
    mockMvc.perform(get("/organizations")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanListOrganizations() throws Exception {
    mockMvc
        .perform(get("/organizations").with(auth("stu", "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void studentCannotCreateOrganization() throws Exception {
    mockMvc
        .perform(
            post("/organizations").with(auth("stu", "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("Acme"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCreatesOrganization() throws Exception {
    String body = objectMapper.writeValueAsString(payload("Globex"));
    String response =
        mockMvc
            .perform(
                post("/organizations").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("Globex"))
            .andExpect(jsonPath("$.data.active").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    UUID id = UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
    assertThat(organizationRepository.findById(id)).isPresent();
  }

  @Test
  void duplicateNameReturnsConflict() throws Exception {
    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("Initech"))))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload("Initech"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(OrganizationService.DUPLICATE_ORGANIZATION));
  }

  @Test
  void validationRejectsBlankName() throws Exception {
    String body = "{\"name\":\"\"}";
    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminUpdatesAndDeletesOrganization() throws Exception {
    String created =
        mockMvc
            .perform(
                post("/organizations").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload("Umbrella"))))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    Map<String, Object> update = payload("Umbrella");
    update = new java.util.LinkedHashMap<>(update);
    update.put("active", false);
    mockMvc
        .perform(
            put("/organizations/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.active").value(false));

    mockMvc
        .perform(delete("/organizations/" + id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(organizationRepository.findById(id)).isEmpty();
  }

  @Test
  void getMissingOrganizationReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/organizations/" + UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void industriesReturnsDistinctSortedValues() throws Exception {
    Map<String, Object> a = new java.util.LinkedHashMap<>(payload("Zeta"));
    a.put("industry", "Technology");
    Map<String, Object> b = new java.util.LinkedHashMap<>(payload("Alpha"));
    b.put("industry", "Health");
    Map<String, Object> c = new java.util.LinkedHashMap<>(payload("Beta"));
    c.put("industry", "Technology");

    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(a)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(b)))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(c)))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/organizations/industries").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items[0]").value("Health"))
        .andExpect(jsonPath("$.data.items[1]").value("Technology"));
  }

  @Test
  void industriesRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/organizations/industries")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminRoundTripsPositionsField() throws Exception {
    Map<String, Object> p = new java.util.LinkedHashMap<>(payload("PositionsRUs"));
    p.put("positions", 24);
    mockMvc
        .perform(
            post("/organizations").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(p)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.positions").value(24));
  }

  @Test
  void statsReflectsOrganizationsAndInternships() throws Exception {
    long beforeOrganizations =
        objectMapper
            .readTree(statsBody())
            .path("data").path("organizationCount").asLong();
    long beforePositions =
        objectMapper
            .readTree(statsBody())
            .path("data").path("availablePositions").asLong();

    UUID appUser = UUID.fromString("12222222-2222-2222-2222-222222222222");
    UUID studentId = UUID.fromString("13333333-3333-3333-3333-333333333333");
    UUID orgA = UUID.fromString("14444444-4444-4444-4444-444444444444");
    UUID orgB = UUID.fromString("15555555-5555-5555-5555-555555555555");
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, 'statstu', 'stat@example.com', 'Stat Stu', 'STUDENT', TRUE, ?)",
        appUser, appUser);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-STAT', 'Stat Stu', 'CS', ?)",
        studentId, appUser, appUser);
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, positions, created_by) VALUES (?, 'Apex One', 10, ?)",
        orgA, appUser);
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, positions, created_by) VALUES (?, 'Apex Two', 5, ?)",
        orgB, appUser);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, title, status, created_by) "
            + "VALUES (?, ?, ?, 'A', 'APPROVED', ?)",
        UUID.randomUUID(), studentId, orgA, appUser);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, title, status, created_by) "
            + "VALUES (?, ?, ?, 'B', 'COMPLETED', ?)",
        UUID.randomUUID(), studentId, orgB, appUser);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, title, status, created_by) "
            + "VALUES (?, ?, ?, 'C', 'PENDING', ?)",
        UUID.randomUUID(), studentId, orgA, appUser);

    com.fasterxml.jackson.databind.JsonNode after =
        objectMapper.readTree(
            mockMvc
                .perform(get("/organizations/stats").with(auth("admin", "ADMIN")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
            .path("data");
    assertThat(after.path("organizationCount").asLong()).isEqualTo(beforeOrganizations + 2);
    assertThat(after.path("activeOrganizations").asLong()).isEqualTo(beforeOrganizations + 2);
    assertThat(after.path("availablePositions").asLong()).isEqualTo(beforePositions + 15);
    assertThat(after.path("studentsPlaced").asLong()).isEqualTo(2);
    assertThat(after.path("activeInterns").asLong()).isEqualTo(1);
  }

  private String statsBody() throws Exception {
    return mockMvc
        .perform(get("/organizations/stats").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }
}
