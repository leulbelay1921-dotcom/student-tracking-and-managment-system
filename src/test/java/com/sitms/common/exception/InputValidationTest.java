package com.sitms.common.exception;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Input-validation consistency (SITMS-055): every invalid request must return the standard
 * {@code ApiResponse} envelope with a stable machine-readable {@code errorCode}, regardless of
 * whether the failure is bean validation, unreadable JSON, or a type mismatch.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InputValidationTest {

  private static final String ADMIN_SUB = "admin";

  @Autowired private MockMvc mockMvc;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private RequestPostProcessor admin() {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, ADMIN_SUB)
            .claim("preferred_username", "user-" + ADMIN_SUB)
            .claim("realm_access", Map.of("roles", List.of("ADMIN")))
            .build();
    return authentication(jwtAuthenticationConverter.convert(jwt));
  }

  @Test
  void beanValidationReturnsConsistentEnvelope() throws Exception {
    String body =
        "{\"appUserUsername\":\"\",\"studentNo\":\"\",\"fullName\":\"\","
            + "\"program\":\"\",\"level\":0}";
    mockMvc
        .perform(
            post("/students")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void malformedJsonReturnsConsistentEnvelope() throws Exception {
    mockMvc
        .perform(post("/students").with(admin()).contentType(MediaType.APPLICATION_JSON).content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"))
        .andExpect(jsonPath("$.message").isNotEmpty());
  }

  @Test
  void missingBodyReturnsConsistentEnvelope() throws Exception {
    mockMvc
        .perform(post("/students").with(admin()).contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void pathVariableTypeMismatchReturnsConsistentEnvelope() throws Exception {
    mockMvc
        .perform(get("/students/not-a-uuid").with(admin()))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST"));
  }

  @Test
  void nonAdminRejectedByRoleGateBeforeValidation() throws Exception {
    // A non-admin must be rejected by the role gate (403) even with a valid-looking body,
    // proving authorization is evaluated independently of validation.
    String body =
        "{\"appUserUsername\":\"fk-invalid\",\"studentNo\":\"S-1\",\"fullName\":\"A A\","
            + "\"program\":\"CS\",\"level\":1}";
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, "student")
            .claim("preferred_username", "user-student")
            .claim("realm_access", Map.of("roles", List.of("STUDENT")))
            .build();
    mockMvc
        .perform(
            post("/students")
                .with(authentication(jwtAuthenticationConverter.convert(jwt)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isForbidden());
  }
}
