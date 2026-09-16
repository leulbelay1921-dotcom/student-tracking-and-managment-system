package com.sitms.activity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link ActivityController} (SITMS-028): RBAC, CRUD, submit/review. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ActivityControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID FACULTY_APP_USER = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
  private static final UUID FACULTY_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID OTHER_SUPERVISOR_APP_USER =
      UUID.fromString("aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private ActivityRepository activityRepository;
  @Autowired private ActivityReviewRepository reviewRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_APP_USER,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        SUPERVISOR_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        FACULTY_ID,
        STUDENT_APP_USER);
    seedAppUser(FACULTY_APP_USER, "faculty1", "FACULTY");
    jdbcTemplate.update(
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, title, department, created_by) "
            + "VALUES (?, ?, 'A-1', 'Faculty One', 'Dr', 'CS', ?)",
        FACULTY_ID,
        FACULTY_APP_USER,
        STUDENT_APP_USER);
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
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'Other Internship', ?)",
        UUID.randomUUID(),
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
        FACULTY_ID,
        OTHER_STUDENT_APP_USER);
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
        "internshipId", INTERNSHIP_ID.toString(),
        "activityDate", "2026-09-01",
        "type", "DAILY",
        "description", "Worked on the reporting module",
        "hours", 3.5);
  }

  private UUID createActivity() throws Exception {
    return createActivityFor(STUDENT_ID);
  }

  private UUID createActivityFor(UUID studentId) throws Exception {
    Map<String, Object> p = new java.util.LinkedHashMap<>(payload());
    p.put("studentId", studentId.toString());
    String response =
        mockMvc
            .perform(
                post("/activities").with(auth("admin", "ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(p)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
  }

  @Test
  void anonymousCannotListActivities() throws Exception {
    mockMvc.perform(get("/activities")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanListActivities() throws Exception {
    mockMvc
        .perform(get("/activities").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void studentCreatesOwnActivityAsDraft() throws Exception {
    mockMvc
        .perform(
            post("/activities")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("DRAFT"))
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()));

    var activity = activityRepository.findByStudentId(STUDENT_ID, PageRequest.of(0, 10)).getContent().get(0);
    assertThat(activity.getStudentId()).isEqualTo(STUDENT_ID);
  }

  @Test
  void studentCreatesOwnActivityWithoutStudentIdViaSelfService() throws Exception {
    // The UI activity form is self-service and does not carry studentId; the
    // backend must resolve the profile from the authenticated user (SITMS-062).
    var request = new java.util.LinkedHashMap<String, Object>(payload());
    request.remove("studentId");
    mockMvc
        .perform(
            post("/activities")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.status").value("DRAFT"));
  }

  @Test
  void adminMustProvideStudentIdWhenCreatingOnBehalf() throws Exception {
    var request = new java.util.LinkedHashMap<String, Object>(payload());
    request.remove("studentId");
    mockMvc
        .perform(
            post("/activities")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_PROFILE_NOT_FOUND"));
  }

  @Test
  void studentWithoutProfileCannotCreate() throws Exception {
    var noProfile = UUID.fromString("99999999-9999-9999-9999-999999999999");
    mockMvc
        .perform(
            post("/activities").with(auth(noProfile.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_PROFILE_NOT_FOUND"));
  }

  @Test
  void adminCreatesActivityAsDraft() throws Exception {
    UUID id = createActivity();
    Activity activity = activityRepository.findById(id).orElseThrow();
    assertThat(activity.getDescription()).isEqualTo("Worked on the reporting module");
    assertThat(activity.getStatus()).isEqualTo(ActivityStatus.DRAFT);
  }

  @Test
  void studentSubmitsOwnActivity() throws Exception {
    UUID id = createActivity();

    mockMvc
        .perform(
            post("/activities/" + id + "/submit")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
  }

  @Test
  void studentSubmitsOwnActivityWithoutInternship() throws Exception {
    // Self-service activities are logged before any internship is assigned, so the
    // activity has internshipId = null; submitting must not NPE in the supervisor
    // notification lookup (SITMS-062 / E2E activity journey).
    var request = new java.util.LinkedHashMap<String, Object>(payload());
    request.remove("internshipId");
    request.remove("studentId");
    String created =
        mockMvc
            .perform(
                post("/activities").with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID id = UUID.fromString(objectMapper.readTree(created).path("data").path("id").asText());

    mockMvc
        .perform(
            post("/activities/" + id + "/submit")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("SUBMITTED"));

    var persisted = activityRepository.findById(id).orElseThrow();
    assertThat(persisted.getInternshipId()).isNull();
  }

  @Test
  void supervisorReviewsSubmittedActivityAsApproved() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(
            post("/activities/" + id + "/submit")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"feedback\":\"Great work\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_APPROVED"));

    ActivityReview review = reviewRepository.findByActivityId(id).orElseThrow();
    assertThat(review.getDecision()).isEqualTo(ReviewDecision.APPROVED);
    assertThat(review.getFeedback()).isEqualTo("Great work");
    assertThat(review.getReviewerId()).isEqualTo(SUPERVISOR_ID);
  }

  @Test
  void supervisorReviewQueueShowsOnlyAssignedStudents() throws Exception {
    UUID assigned = createActivity();
    UUID unassigned = createActivityFor(OTHER_STUDENT_ID);

    mockMvc
        .perform(post("/activities/" + assigned + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + unassigned + "/submit")
                .with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            get("/activities/review-queue")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(assigned.toString()));
  }

  @Test
  void adminSeesAllSubmittedInReviewQueue() throws Exception {
    UUID assigned = createActivity();
    UUID unassigned = createActivityFor(OTHER_STUDENT_ID);
    mockMvc
        .perform(post("/activities/" + assigned + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + unassigned + "/submit")
                .with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(get("/activities/review-queue").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2));
  }

  @Test
  void adminCanReviewAnySubmittedActivity() throws Exception {
    UUID unassigned = createActivityFor(OTHER_STUDENT_ID);
    mockMvc
        .perform(
            post("/activities/" + unassigned + "/submit")
                .with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + unassigned + "/review")
                .with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"feedback\":\"ok\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_APPROVED"));
  }

  @Test
  void supervisorCannotReviewUnassignedActivity() throws Exception {
    UUID unassigned = createActivityFor(OTHER_STUDENT_ID);
    mockMvc
        .perform(
            post("/activities/" + unassigned + "/submit")
                .with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + unassigned + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"feedback\":\"nope\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void rejectedActivitySurfacesFeedbackOnRead() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"REJECTED\",\"feedback\":\"Need more detail\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_REJECTED"));

    mockMvc
        .perform(get("/activities/" + id).with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_REJECTED"))
        .andExpect(jsonPath("$.data.review.decision").value("REJECTED"))
        .andExpect(jsonPath("$.data.review.feedback").value("Need more detail"));
  }

  @Test
  void studentCannotReview() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(
            post("/activities/" + id + "/review").with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void cannotReviewUnsubmittedActivity() throws Exception {
    UUID id = createActivity();

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.INVALID_STATUS_TRANSITION));
  }

  @Test
  void validationRejectsMissingDescription() throws Exception {
    String body =
        "{\"studentId\":\"" + STUDENT_ID + "\",\"activityDate\":\"2026-09-01\",\"type\":\"DAILY\"}";
    mockMvc
        .perform(
            post("/activities").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void adminUpdatesAndDeletesActivity() throws Exception {
    UUID id = createActivity();

    Map<String, Object> update = new java.util.LinkedHashMap<>(payload());
    update.put("description", "Updated activity");
    mockMvc
        .perform(
            put("/activities/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.description").value("Updated activity"));

    mockMvc.perform(delete("/activities/" + id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(activityRepository.findById(id)).isEmpty();
  }

  @Test
  void getMissingActivityReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/activities/" + UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void studentSeesOnlyOwnActivities() throws Exception {
    createActivity();
    createActivityFor(OTHER_STUDENT_ID);

    mockMvc
        .perform(get("/activities").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void studentCannotGetAnotherStudentsActivity() throws Exception {
    UUID otherId = createActivityFor(OTHER_STUDENT_ID);

    mockMvc
        .perform(get("/activities/" + otherId).with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.ACTIVITY_NOT_FOUND));
  }

  @Test
  void supervisorSeesOnlyAssignedStudentsActivities() throws Exception {
    createActivity();
    UUID otherId = createActivityFor(OTHER_STUDENT_ID);

    mockMvc
        .perform(get("/activities").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));

    mockMvc
        .perform(
            get("/activities/" + otherId).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.ACTIVITY_NOT_FOUND));
  }

  @Test
  void supervisorCanViewAnotherSupervisorsStudentActivities() throws Exception {
    createActivity();
    createActivityFor(OTHER_STUDENT_ID);

    mockMvc
        .perform(
            get("/activities")
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString())
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].studentId").value(OTHER_STUDENT_ID.toString()));

    mockMvc
        .perform(get("/activities").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].studentId").value(STUDENT_ID.toString()));
  }

  // ---- Two-stage review tests (SITMS-028 / SITMS-031) ----

  @Test
  void supervisorCompanyApprovalTransitionsToCompanyApproved() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"feedback\":\"Looks good\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_APPROVED"));

    Activity persisted = activityRepository.findById(id).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(ActivityStatus.COMPANY_APPROVED);
  }

  @Test
  void companyRejectedRequiresReason() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"REJECTED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.REJECT_REASON_REQUIRED));
  }

  @Test
  void facultyCanReviewCompanyApprovedActivity() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"feedback\":\"ok\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\",\"feedback\":\"Great\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("APPROVED"))
        .andExpect(jsonPath("$.data.academicReview.feedback").value("Great"));
  }

  @Test
  void facultyRequestingRevisionTransitionsToRevisionRequested() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"REJECTED\",\"feedback\":\"Please revise section 3\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REVISION_REQUESTED"))
        .andExpect(jsonPath("$.data.academicReview.feedback").value("Please revise section 3"));

    Activity persisted = activityRepository.findById(id).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(ActivityStatus.REVISION_REQUESTED);
    assertThat(persisted.getAcademicFeedback()).isEqualTo("Please revise section 3");
  }

  @Test
  void facultyRevisionRequiresReason() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"REJECTED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.REJECT_REASON_REQUIRED));
  }

  @Test
  void studentCanEditRevisionRequestedActivity() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"REJECTED\",\"feedback\":\"Revise\"}"))
        .andExpect(status().isOk());

    var update = new java.util.LinkedHashMap<String, Object>(payload());
    update.put("description", "Revised after feedback");
    mockMvc
        .perform(
            put("/activities/" + id)
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.description").value("Revised after feedback"));

    Activity persisted = activityRepository.findById(id).orElseThrow();
    assertThat(persisted.getStatus()).isEqualTo(ActivityStatus.REVISION_REQUESTED);
  }

  @Test
  void studentCannotEditCompanyApprovedActivity() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    var update = new java.util.LinkedHashMap<String, Object>(payload());
    update.put("description", "Should not work");
    mockMvc
        .perform(
            put("/activities/" + id)
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.INVALID_STATUS_TRANSITION));
  }

  @Test
  void companyRejectedCannotBeFurtherReviewed() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"REJECTED\",\"feedback\":\"No\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPANY_REJECTED"));

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(FACULTY_APP_USER.toString(), "FACULTY"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(ActivityService.INVALID_STATUS_TRANSITION));
  }

  @Test
  void studentCannotReviewCompanyApprovedActivity() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorCannotReviewAcademicStage() throws Exception {
    UUID id = createActivity();
    mockMvc
        .perform(post("/activities/" + id + "/submit").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isOk());

    mockMvc
        .perform(
            post("/activities/" + id + "/review")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVED\"}"))
        .andExpect(status().isForbidden());
  }
}
