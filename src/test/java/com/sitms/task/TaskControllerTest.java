package com.sitms.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link TaskController} (SITMS-024): RBAC, CRUD, status lifecycle. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class TaskControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("99999999-9999-9999-9999-999999999999");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private TaskRepository taskRepository;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
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
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_APP_USER,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        STUDENT_APP_USER);
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
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("studentId", STUDENT_ID.toString());
    body.put("title", "Write unit tests");
    body.put("description", "Cover task entity");
    body.put("category", "Testing");
    body.put("expectedOutcome", "Covered suite");
    return body;
  }

  /** Create a task via the API as the company supervisor and return its id. */
  private UUID createTask() throws Exception {
    return createTask(payload());
  }

  private UUID createTask(Map<String, Object> body) throws Exception {
    String response =
        mockMvc
            .perform(
                post("/tasks")
                    .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(response).path("data").path("id").asText());
  }

  private void patchStatus(UUID id, String sub, String role, String status) throws Exception {
    mockMvc
        .perform(
            patch("/tasks/" + id + "/status")
                .with(auth(sub, role))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + status + "\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void anonymousCannotListTasks() throws Exception {
    mockMvc.perform(get("/tasks")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanListTasks() throws Exception {
    mockMvc
        .perform(get("/tasks").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true));
  }

  @Test
  void studentListIsScopedToOwnTasks() throws Exception {
    createTask();

    mockMvc
        .perform(get("/tasks").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1));
  }

  @Test
  void supervisorListIsScopedToOwnTasks() throws Exception {
    createTask();

    mockMvc
        .perform(get("/tasks").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1));
  }

  @Test
  void adminSeesAllTasksRegardlessOfScope() throws Exception {
    createTask();

    mockMvc
        .perform(get("/tasks").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1));
  }

  @Test
  void supervisorCreatesTaskWithAssignedStatusAndDefaultMediumPriority() throws Exception {
    UUID id = createTask();
    Task task = taskRepository.findById(id).orElseThrow();
    assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
    assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
    assertThat(task.getSupervisorId()).isEqualTo(SUPERVISOR_ID);
    assertThat(task.getCategory()).isEqualTo("Testing");
    assertThat(task.getExpectedOutcome()).isEqualTo("Covered suite");
  }

  @Test
  void filtersTasksByPriority() throws Exception {
    Map<String, Object> high = new LinkedHashMap<>(payload());
    high.put("title", "Urgent task");
    high.put("priority", "URGENT");
    createTask(high);

    createTask(); // defaults to MEDIUM

    mockMvc
        .perform(
            get("/tasks")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("priority", "URGENT"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1));
  }

  @Test
  void searchesTasksByTitle() throws Exception {
    createTask(); // title "Write unit tests"

    mockMvc
        .perform(get("/tasks").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")).param("q", "unit"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(1));

    mockMvc
        .perform(get("/tasks").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")).param("q", "nomatch"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.items.length()").value(0));
  }

  @Test
  void studentCannotCreateTask() throws Exception {
    mockMvc
        .perform(
            post("/tasks").with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminCannotCreateUpdateOrDeleteTask() throws Exception {
    UUID id = createTask();

    mockMvc
        .perform(
            post("/tasks").with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            put("/tasks/" + id).with(auth("admin", "ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isForbidden());

    mockMvc.perform(delete("/tasks/" + id).with(auth("admin", "ADMIN"))).andExpect(status().isForbidden());
    assertThat(taskRepository.findById(id)).isPresent();
  }

  @Test
  void supervisorCannotAssignTaskToUnassignedStudent() throws Exception {
    Map<String, Object> body = new LinkedHashMap<>(payload());
    body.put("studentId", OTHER_STUDENT_ID.toString());
    mockMvc
        .perform(
            post("/tasks").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
        .andExpect(status().isForbidden());
  }

  @Test
  void supervisorCanReviewCompletedTaskButNotCompleteIt() throws Exception {
    UUID id = createTask();
    patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "IN_PROGRESS");
    patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "COMPLETED");

    mockMvc
        .perform(
            patch("/tasks/" + id + "/status")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVIEWED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REVIEWED"));
  }

  @Test
  void supervisorCannotPatchACompletedTaskDirectlyBackwardsOrCompleteIt() throws Exception {
    UUID id = createTask();

    mockMvc
        .perform(
            patch("/tasks/" + id + "/status")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void studentCanMarkInProgressThenComplete() throws Exception {
    UUID id = createTask();

    patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "IN_PROGRESS");

    mockMvc
        .perform(
            patch("/tasks/" + id + "/status")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"COMPLETED\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("COMPLETED"));
  }

  @Test
  void studentCannotSkipToReviewed() throws Exception {
    UUID id = createTask();
    patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "COMPLETED");

    mockMvc
        .perform(
            patch("/tasks/" + id + "/status")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVIEWED\"}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void invalidStatusTransitionReturnsBadRequest() throws Exception {
    UUID id = createTask();

    // ASSIGNED -> REVIEWED is role-legal for a supervisor but not a valid transition.
    mockMvc
        .perform(
            patch("/tasks/" + id + "/status")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"REVIEWED\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(TaskService.INVALID_STATUS_TRANSITION));
  }

  @Test
  void validationRejectsMissingStudentId() throws Exception {
    String body = "{\"title\":\"T\"}";
    mockMvc
        .perform(
            post("/tasks").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  void supervisorUpdatesAndDeletesOwnTask() throws Exception {
    UUID id = createTask();

    Map<String, Object> update = new LinkedHashMap<>(payload());
    update.put("title", "Updated Task");
    mockMvc
        .perform(
            put("/tasks/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(update)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.title").value("Updated Task"));

    mockMvc
        .perform(delete("/tasks/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk());
    assertThat(taskRepository.findById(id)).isEmpty();
  }

  @Test
  void supervisorCannotDeleteACompletedTask() throws Exception {
    UUID id = createTask();
    patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "COMPLETED");

    mockMvc
        .perform(delete("/tasks/" + id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value(TaskService.COMPLETED_TASK_DELETION));
  }

  @Test
  void nonOwnerSupervisorCannotEditOrDelete() throws Exception {
    UUID id = createTask();
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'sup2', 'sup2@example.com', 'Sup Two', "
            + "'SUPERVISOR', TRUE, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb')");
    UUID otherSupervisorId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', ?, 'EMP-2', 'Sup Two', ?)",
        otherSupervisorId,
        ORGANIZATION_ID,
        SUPERVISOR_APP_USER);

    mockMvc
        .perform(
            put("/tasks/" + id)
                .with(auth("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload())))
        .andExpect(status().isForbidden());

    mockMvc
        .perform(
            delete("/tasks/" + id).with(auth("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb", "SUPERVISOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void getMissingTaskReturnsNotFound() throws Exception {
    mockMvc
        .perform(get("/tasks/" + UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound());
  }

  @Test
  void progressComputesPercentForStudent() throws Exception {
    createTaskAtStatus("A", "COMPLETED");
    createTaskAtStatus("B", "COMPLETED");
    createTaskAtStatus("C", "IN_PROGRESS");
    createTaskAtStatus("D", "ASSIGNED");

    mockMvc
        .perform(get("/tasks/progress").with(auth("admin", "ADMIN")).param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalTasks").value(4))
        .andExpect(jsonPath("$.data.completedTasks").value(2))
        .andExpect(jsonPath("$.data.inProgressTasks").value(1))
        .andExpect(jsonPath("$.data.pendingTasks").value(1))
        .andExpect(jsonPath("$.data.percent").value(50.0));
  }

  @Test
  void progressScopesToInternship() throws Exception {
    createTaskAtStatus("I-A", "COMPLETED", INTERNSHIP_ID);
    createTaskAtStatus("I-B", "ASSIGNED", INTERNSHIP_ID);

    mockMvc
        .perform(get("/tasks/progress").with(auth("admin", "ADMIN")).param("internshipId", INTERNSHIP_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalTasks").value(2))
        .andExpect(jsonPath("$.data.completedTasks").value(1))
        .andExpect(jsonPath("$.data.percent").value(50.0));
  }

  @Test
  void progressReturnsZeroWhenNoTasks() throws Exception {
    mockMvc
        .perform(get("/tasks/progress").with(auth("admin", "ADMIN")).param("studentId", STUDENT_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalTasks").value(0))
        .andExpect(jsonPath("$.data.percent").value(0.0));
  }

  @Test
  void studentProgressDefaultsToOwnProfile() throws Exception {
    createTaskAtStatus("A", "COMPLETED");

    mockMvc
        .perform(get("/tasks/progress").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.totalTasks").value(1));
  }

  @Test
  void studentCannotReadAnotherStudentsProgress() throws Exception {
    mockMvc
        .perform(
            get("/tasks/progress")
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT"))
                .param("studentId", UUID.randomUUID().toString()))
        .andExpect(status().isForbidden());
  }

  /** Creates a task as the company supervisor, then drives it to the requested status. */
  private UUID createTaskAtStatus(String title, String status) throws Exception {
    return createTaskAtStatus(title, status, null);
  }

  private UUID createTaskAtStatus(String title, String status, UUID internshipId) throws Exception {
    Map<String, Object> p = new LinkedHashMap<>(payload());
    p.put("title", title);
    if (internshipId != null) {
      p.put("internshipId", internshipId.toString());
    }
    UUID id = createTask(p);
    if ("IN_PROGRESS".equals(status)) {
      patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "IN_PROGRESS");
    } else if ("COMPLETED".equals(status)) {
      patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "IN_PROGRESS");
      patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "COMPLETED");
    } else if ("REVIEWED".equals(status)) {
      patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "IN_PROGRESS");
      patchStatus(id, STUDENT_APP_USER.toString(), "STUDENT", "COMPLETED");
      patchStatus(id, SUPERVISOR_APP_USER.toString(), "SUPERVISOR", "REVIEWED");
    }
    return id;
  }
}