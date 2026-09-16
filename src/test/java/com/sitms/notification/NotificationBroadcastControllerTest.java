package com.sitms.notification;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** Integration tests for the notification broadcast (POST /notifications/broadcast). */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class NotificationBroadcastControllerTest {

  private static final UUID ADMIN = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_1 = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_2 = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID STAFF_USER = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private MockMvc mockMvc;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private NotificationRepository notificationRepository;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private UUID organizationId;
  private UUID student1Profile;
  private UUID student2Profile;
  private UUID supervisorProfile;
  private UUID staffProfile;

  @BeforeEach
  void seed() {
    seedAppUser(ADMIN, "admin", "ADMIN");
    seedAppUser(STUDENT_1, "student1", "STUDENT");
    seedAppUser(STUDENT_2, "student2", "STUDENT");
    seedAppUser(SUPERVISOR_USER, "sup1", "COMPANY_SUPERVISOR");
    seedAppUser(STAFF_USER, "fac1", "UNIVERSITY_SUPERVISOR");

    student1Profile = seedStudent(STUDENT_1, "S001", "Anna Alpha");
    student2Profile = seedStudent(STUDENT_2, "S002", "Ben Beta");
    organizationId = seedOrganization("Acme Corp");
    supervisorProfile = seedSupervisor(SUPERVISOR_USER, organizationId);
    staffProfile = seedStaff(STAFF_USER);
    seedInternship(student2Profile, organizationId, supervisorProfile);
  }

  private void seedAppUser(UUID id, String username, String role) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, ?, TRUE, ?)",
        id, username, username + "@example.com", username, role, ADMIN);
  }

  private UUID seedStudent(UUID appUserId, String studentNo, String name) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, level, created_by) "
            + "VALUES (?, ?, ?, ?, 'CS', 2, ?)",
        id, appUserId, studentNo, name, ADMIN);
    return id;
  }

  private UUID seedOrganization(String name) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, active, created_by) VALUES (?, ?, TRUE, ?)",
        id, name, ADMIN);
    return id;
  }

  private UUID seedSupervisor(UUID appUserId, UUID orgId) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, active, created_by) "
            + "VALUES (?, ?, ?, ?, 'Cooper Supervisor', TRUE, ?)",
        id, appUserId, orgId, "EMP1", ADMIN);
    return id;
  }

  private UUID seedStaff(UUID appUserId) {
    UUID id = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, active, created_by) "
            + "VALUES (?, ?, ?, ?, TRUE, ?)",
        id, appUserId, "STF1", "Fay Faculty", ADMIN);
    return id;
  }

  private void seedInternship(UUID studentId, UUID orgId, UUID supervisorId) {
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'APPROVED', ?)",
        UUID.randomUUID(), studentId, orgId, supervisorId, "Summer internship", ADMIN);
  }

  private long broadcastCount() {
    return notificationRepository.count();
  }

  private long notificationsFor(UUID recipientId) {
    return notificationRepository.countByRecipientIdAndRead(recipientId, false);
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

  private String payload(Audience audience, UUID recipientId, String title) {
    return """
        {
          "audience": "%s",
          "recipientId": %s,
          "title": "%s",
          "body": "Body text",
          "link": "/internships"
        }
        """
        .formatted(
            audience,
            recipientId == null ? "null" : "\"" + recipientId + "\"",
            title);
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder broadcast(
      UUID actor, String role, Audience audience, UUID recipientId, String title) {
    return post("/notifications/broadcast")
        .with(auth(actor.toString(), role))
        .contentType(APPLICATION_JSON)
        .content(payload(audience, recipientId, title));
  }

  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder broadcastUnauthenticated(
      Audience audience, UUID recipientId, String title) {
    return post("/notifications/broadcast")
        .contentType(APPLICATION_JSON)
        .content(payload(audience, recipientId, title));
  }

  @Test
  void unauthenticatedRequestIsRejected() throws Exception {
    mockMvc
        .perform(broadcastUnauthenticated(Audience.ALL_STUDENTS, null, "Hi"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void authenticatedRolesMayBroadcast() throws Exception {
    record Actor(UUID id, String role) {}

    List<Actor> actors =
        List.of(
            new Actor(STUDENT_1, "STUDENT"),
            new Actor(SUPERVISOR_USER, "COMPANY_SUPERVISOR"),
            new Actor(STAFF_USER, "UNIVERSITY_SUPERVISOR"),
            new Actor(ADMIN, "ADMIN"));

    for (Actor actor : actors) {
      mockMvc
          .perform(broadcast(actor.id(), actor.role(), Audience.ALL_STUDENTS, null, "Hi everyone"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.data.recipients").value(2));
    }
  }

  @Test
  void broadcastsToCompanySupervisors() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.COMPANY_SUPERVISORS, null, "Supervisor notice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.recipients").value(1));

    org.assertj.core.api.Assertions.assertThat(notificationsFor(SUPERVISOR_USER)).isEqualTo(1);
  }

  @Test
  void broadcastsToUniversitySupervisors() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.UNIVERSITY_SUPERVISORS, null, "Faculty notice"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.recipients").value(1));

    org.assertj.core.api.Assertions.assertThat(notificationsFor(STAFF_USER)).isEqualTo(1);
  }

  @Test
  void broadcastsToASpecificStudent() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.STUDENT, student1Profile, "For Anna"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.recipients").value(1));

    org.assertj.core.api.Assertions.assertThat(notificationsFor(STUDENT_1)).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(notificationsFor(STUDENT_2)).isZero();
  }

  @Test
  void broadcastsToAnOrganizationReachingSupervisorAndStudents() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.ORGANIZATION, organizationId, "Acme update"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.recipients").value(2));

    org.assertj.core.api.Assertions.assertThat(notificationsFor(SUPERVISOR_USER)).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(notificationsFor(STUDENT_2)).isEqualTo(1);
  }

  @Test
  void specificAudienceRequiresARecipient() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.STUDENT, null, "Missing recipient"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("RECIPIENT_REQUIRED"));
  }

  @Test
  void unknownStudentReturns404() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.STUDENT, UUID.randomUUID(), "No such student"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_NOT_FOUND"));
  }

  @Test
  void unknownOrganizationReturns404() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.ORGANIZATION, UUID.randomUUID(), "No such org"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("ORGANIZATION_NOT_FOUND"));
  }

  @Test
  void emptyTitleIsRejected() throws Exception {
    mockMvc
        .perform(broadcast(ADMIN, "ADMIN", Audience.ALL_STUDENTS, null, ""))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    org.assertj.core.api.Assertions.assertThat(broadcastCount()).isZero();
  }
}