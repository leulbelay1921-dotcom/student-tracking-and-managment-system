package com.sitms.common.security;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies resource-server authorization (SITMS-005): anonymous requests are rejected, a valid JWT
 * is mapped to {@code ROLE_*} authorities, and URL role rules deny non-privileged callers.
 *
 * <p>Paths are context-relative (the app serves under {@code server.servlet.context-path=/api}),
 * matching how MockMvc routes requests against the controllers' {@code /me}, {@code /admin/**} etc.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityConfigTest {

  

  @Autowired private MockMvc mockMvc;

  @Autowired
  private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private static Jwt buildJwt(String sub, String... roles) {
    return Jwt.withTokenValue("token")
        .header("alg", "none")
        .claim(JwtClaimNames.SUB, sub)
        .claim("preferred_username", "user-" + sub)
        .claim("realm_access", java.util.Map.of("roles", List.of(roles)))
        .build();
  }

  private org.springframework.test.web.servlet.request.RequestPostProcessor auth(String sub,
      String... roles) {
    return authentication(jwtAuthenticationConverter.convert(buildJwt(sub, roles)));
  }

  @Test
  void anonymousRequestIsUnauthorized() throws Exception {
    mockMvc.perform(get("/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void adminTokenReachesMeEndpointWithRole() throws Exception {
    mockMvc
        .perform(get("/me").with(auth("admin-uuid", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"));
  }

  @Test
  void studentTokenReachesMeEndpointWithStudentRole() throws Exception {
    mockMvc
        .perform(get("/me").with(auth("student-uuid", "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.roles[0]").value("ROLE_STUDENT"));
  }

  @Test
  void studentIsForbiddenFromAdminNamespace() throws Exception {
    mockMvc
        .perform(get("/admin/students").with(auth("student-uuid", "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminPassesAuthorizationOnAdminNamespace() throws Exception {
    // Admin satisfies the /admin/** rule. No admin controller exists yet, so the request is not
    // forbidden (it clears the role gate) — a 403 here would mean authorization wrongly denied him.
    mockMvc
        .perform(get("/admin/students").with(auth("admin-uuid", "ADMIN")))
        .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
            result.getResponse().getStatus()).isNotEqualTo(403));
  }

  @Test
  void supervisorIsAllowedFromSupervisorNamespace() throws Exception {
    mockMvc
        .perform(get("/supervisor/x").with(auth("sup-uuid", "SUPERVISOR")))
        .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
            result.getResponse().getStatus()).isNotEqualTo(403));
  }

  @Test
  void corsPreflightForFrontendOriginIsAllowed() throws Exception {
    // The Angular SPA (http://localhost:4200) must be able to call the API from the
    // browser; a missing .cors(...) in the security chain rejects preflights with 401
    // (SITMS-062). The preflight must pass AND carry the allow-origin header.
    mockMvc
        .perform(
            options("/api/internships")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization"))
        .andExpect(status().isOk())
        .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
            result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isEqualTo("http://localhost:4200"));
  }

  @Test
  void corsRejectsUnknownOrigin() throws Exception {
    mockMvc
        .perform(
            options("/api/internships")
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "GET"))
        .andExpect(result -> org.assertj.core.api.Assertions.assertThat(
            result.getResponse().getHeader("Access-Control-Allow-Origin"))
                .isNull());
  }
}
