package com.sitms.notification;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link NotificationController} (SITMS-051): list, unread count, mark-read. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationControllerTest {

  private static final String ADMIN_USERNAME = "admin";
  private static final UUID ADMIN_APP_USER = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_STUDENT_APP_USER =
      UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private UUID studentUnreadNotification;
  private UUID studentReadNotification;
  private UUID otherStudentNotification;
  private UUID adminNotification;

  @BeforeEach
  void seed() {
    seedAppUser(ADMIN_APP_USER, ADMIN_USERNAME, "ADMIN");
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
    studentUnreadNotification =
        seedNotification(STUDENT_APP_USER, "ACTIVITY_REVIEWED", "Activity approved", false, "-1 hours");
    studentReadNotification =
        seedNotification(STUDENT_APP_USER, "EVALUATION_SUBMITTED", "Evaluation ready", true, "-2 hours");
    otherStudentNotification =
        seedNotification(OTHER_STUDENT_APP_USER, "ACTIVITY_SUBMITTED", "Log submitted", false, "-3 hours");
    adminNotification =
        seedNotification(ADMIN_APP_USER, "DOCUMENT_UPLOADED", "Document uploaded", false, "-4 hours");
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

  private UUID seedNotification(
      UUID recipientId, String type, String title, boolean read, String createdOffset) {
    UUID id = UUID.randomUUID();
    java.sql.Timestamp ts = computeOffsetTimestamp(createdOffset);
    jdbcTemplate.update(
        "INSERT INTO notification (id, recipient_id, type, title, body, link, read, created_by, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, 'body', '/activities/1', ?, ?, ?, CURRENT_TIMESTAMP)",
        id,
        recipientId,
        type,
        title,
        read,
        STUDENT_APP_USER,
        ts);
    return id;
  }

  private static java.sql.Timestamp computeOffsetTimestamp(String offset) {
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    String trimmed = offset.trim();
    int value = Integer.parseInt(trimmed.split("\\s+")[0]);
    if (trimmed.contains("hour")) {
      now = now.plusHours(value);
    } else if (trimmed.contains("minute")) {
      now = now.plusMinutes(value);
    }
    return java.sql.Timestamp.valueOf(now);
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
  void unauthenticatedRequestIsRejected() throws Exception {
    mockMvc.perform(get("/notifications")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentListsOwnNotificationsNewestFirst() throws Exception {
    mockMvc
        .perform(get("/notifications").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items[0].id").value(studentUnreadNotification.toString()))
        .andExpect(jsonPath("$.data.items[1].id").value(studentReadNotification.toString()))
        .andExpect(jsonPath("$.data.items[0].type").value("ACTIVITY_REVIEWED"))
        .andExpect(jsonPath("$.data.items[0].title").value("Activity approved"))
        .andExpect(jsonPath("$.data.items[0].read").value(false))
        .andExpect(jsonPath("$.data.items[1].read").value(true));
  }

  @Test
  void unreadCountIsScopedToTheCaller() throws Exception {
    mockMvc
        .perform(get("/notifications/unread-count").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(1));
  }

  @Test
  void marksOneNotificationAsRead() throws Exception {
    mockMvc
        .perform(
            patch("/notifications/" + studentUnreadNotification + "/read")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.id").value(studentUnreadNotification.toString()))
        .andExpect(jsonPath("$.data.read").value(true));

    mockMvc
        .perform(get("/notifications/unread-count").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(jsonPath("$.data").value(0));
  }

  @Test
  void marksAllNotificationsAsRead() throws Exception {
    mockMvc
        .perform(
            patch("/notifications/read-all").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").value(1));

    mockMvc
        .perform(get("/notifications/unread-count").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(jsonPath("$.data").value(0));
  }

  @Test
  void cannotMarkAnotherUsersNotification() throws Exception {
    mockMvc
        .perform(
            patch("/notifications/" + otherStudentNotification + "/read")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"));
  }

  @Test
  void unknownNotificationReturns404() throws Exception {
    mockMvc
        .perform(
            patch("/notifications/" + UUID.randomUUID() + "/read")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("NOTIFICATION_NOT_FOUND"));
  }

  @Test
  void adminResolvesNotificationsByAdminUsername() throws Exception {
    mockMvc
        .perform(get("/notifications").with(auth(ADMIN_USERNAME, "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(adminNotification.toString()));
  }
}