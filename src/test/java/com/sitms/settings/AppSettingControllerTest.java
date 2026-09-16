package com.sitms.settings;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Integration tests for {@link AppSettingController} (SITMS-058): RBAC, typed updates, gating. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AppSettingControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ORGANIZATION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedStudent() {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, 'regstu', 'reg@example.com', 'Reg Stu', 'STUDENT', TRUE, ?)",
        STUDENT_APP_USER, STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-REG', 'Reg Stu', 'CS', ?)",
        STUDENT_ID, STUDENT_APP_USER, STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Accendo', ?)",
        ORGANIZATION_ID, STUDENT_APP_USER);
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
  void anonymousCannotListSettings() throws Exception {
    mockMvc.perform(get("/settings")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCannotListSettings() throws Exception {
    mockMvc
        .perform(get("/settings").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminListsSeededSettings() throws Exception {
    mockMvc
        .perform(get("/settings").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(12))
        .andExpect(jsonPath("$.data.totalPages").value(1))
        .andExpect(jsonPath("$.data.items.length()").value(12))
        .andExpect(
            jsonPath("$.data.items[?(@.key=='registration_open')].value").value("true"))
        .andExpect(
            jsonPath("$.data.items[?(@.key=='registration_open')].type").value("BOOLEAN"))
        .andExpect(
            jsonPath("$.data.items[?(@.key=='registration_open')].category").value("GENERAL"))
        .andExpect(
            jsonPath("$.data.items[?(@.key=='mfa_required')].category").value("SECURITY"))
        .andExpect(
            jsonPath(
                    "$.data.items[?(@.key=='email_notifications')].category")
                .value("NOTIFICATIONS"))
        .andExpect(
            jsonPath("$.data.items[?(@.key=='appearance')].category").value("APPEARANCE"));
  }

  @Test
  void anyAuthenticatedUserCanReadPublicAppearanceSetting() throws Exception {
    mockMvc
        .perform(
            get("/settings/public/appearance")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.key").value("appearance"))
        .andExpect(jsonPath("$.data.value").value("solid"))
        .andExpect(jsonPath("$.data.type").value("STRING"));
  }

  @Test
  void anonymousCannotReadPublicSettingBecauseApiRequiresAuthentication() throws Exception {
    mockMvc.perform(get("/settings/public/appearance")).andExpect(status().isUnauthorized());
  }

  @Test
  void nonWhitelistedSettingCannotBeReadPublicly() throws Exception {
    mockMvc
        .perform(
            get("/settings/public/registration_open").with(auth(STUDENT_APP_USER.toString(),
                "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(AppSettingService.SETTING_NOT_FOUND));
  }

  @Test
  void studentCannotReadNonPublicSettingDirectly() throws Exception {
    mockMvc
        .perform(
            get("/settings").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminUpdatesAppearanceSetting() throws Exception {
    mockMvc
        .perform(
            put("/settings/appearance")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"glass\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.value").value("glass"));

    mockMvc
        .perform(get("/settings/public/appearance").with(auth(STUDENT_APP_USER.toString(),
            "STUDENT")))
        .andExpect(jsonPath("$.data.value").value("glass"));
  }

  @Test
  void adminUpdatesBooleanSetting() throws Exception {
    mockMvc
        .perform(
            put("/settings/registration_open")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"false\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.value").value("false"));

    mockMvc
        .perform(get("/settings").with(auth("admin", "ADMIN")))
        .andExpect(
            jsonPath("$.data.items[?(@.key=='registration_open')].value").value("false"));
  }

  @Test
  void rejectsInvalidBooleanValue() throws Exception {
    mockMvc
        .perform(
            put("/settings/registration_open")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"yes\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(AppSettingService.INVALID_SETTING_VALUE));
  }

  @Test
  void rejectsInvalidNumberValue() throws Exception {
    mockMvc
        .perform(
            put("/settings/min_weekly_hours")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"lots\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(AppSettingService.INVALID_SETTING_VALUE));
  }

  @Test
  void unknownSettingReturnsNotFound() throws Exception {
    mockMvc
        .perform(
            put("/settings/does_not_exist")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"x\"}"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(AppSettingService.SETTING_NOT_FOUND));
  }

  @Test
  void closedRegistrationBlocksStudentApplication() throws Exception {
    mockMvc
        .perform(
            put("/settings/registration_open")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"value\":\"false\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/applications")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        Map.of(
                            "organizationId", ORGANIZATION_ID.toString(),
                            "title", "Summer Internship"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("REGISTRATION_CLOSED"));
  }
}