package com.sitms.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link ReportController} (SITMS-044): aggregation and RBAC/scoping. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class ReportControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("aaaaaa00-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("bbbbbb00-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID OTHER_SUPERVISOR_APP_USER = UUID.fromString("dddddd00-dddd-dddd-dddd-dddddddddddd");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("cccccc00-cccc-cccc-cccc-cccccccccccc");
  private static final UUID FACULTY_APP_USER = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final UUID FACULTY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_INTERNSHIP_ID = UUID.fromString("77777777-0000-0000-0000-000000000000");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_APP_USER, "student1", "STUDENT");
    seedAppUser(OTHER_STUDENT_APP_USER, "student2", "STUDENT");
    seedAppUser(SUPERVISOR_APP_USER, "supervisor1", "SUPERVISOR");
    seedAppUser(OTHER_SUPERVISOR_APP_USER, "supervisor2", "SUPERVISOR");
    seedAppUser(FACULTY_APP_USER, "faculty1", "FACULTY");
    jdbcTemplate.update(
        "INSERT INTO academic_staff (id, app_user_id, staff_no, full_name, created_by) "
            + "VALUES (?, ?, 'FAC-1', 'Prof One', ?)",
        FACULTY_ID,
        FACULTY_APP_USER,
        FACULTY_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        SUPERVISOR_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_APP_USER,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'EE', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_APP_USER,
        OTHER_STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        SUPERVISOR_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-2', 'Sup Two', ?)",
        OTHER_SUPERVISOR_ID,
        OTHER_SUPERVISOR_APP_USER,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, academic_staff_id, "
            + "title, status, start_date, end_date, created_by) "
            + "VALUES (?, ?, ?, ?, ?, 'Internship One', 'APPROVED', DATE '2025-01-01', "
            + "DATE '2025-02-01', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
        FACULTY_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, status, "
            + "created_by) VALUES (?, ?, ?, ?, 'Internship Two', 'REJECTED', ?)",
        OTHER_INTERNSHIP_ID,
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
        OTHER_STUDENT_APP_USER);

    jdbcTemplate.update(
        "INSERT INTO activity (id, student_id, internship_id, activity_date, description, hours, "
            + "status, created_by) VALUES (?, ?, ?, DATE '2025-01-10', 'Work', 3, 'APPROVED', ?)",
        UUID.fromString("11111111-0000-0000-0000-000000000001"),
        STUDENT_ID,
        INTERNSHIP_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO activity (id, student_id, internship_id, activity_date, description, hours, "
            + "status, created_by) VALUES (?, ?, ?, DATE '2025-01-11', 'Work', 2, 'SUBMITTED', ?)",
        UUID.fromString("11111111-0000-0000-0000-000000000002"),
        STUDENT_ID,
        INTERNSHIP_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO activity (id, student_id, internship_id, activity_date, description, hours, "
            + "status, created_by) VALUES (?, ?, ?, DATE '2025-01-12', 'Work', 1, 'DRAFT', ?)",
        UUID.fromString("11111111-0000-0000-0000-000000000003"),
        STUDENT_ID,
        INTERNSHIP_ID,
        STUDENT_APP_USER);
    jdbcTemplate.update(
        "INSERT INTO activity (id, student_id, internship_id, activity_date, description, hours, "
            + "status, created_by) VALUES (?, ?, ?, DATE '2025-01-13', 'Work', 4, 'SUBMITTED', ?)",
        UUID.fromString("11111111-0000-0000-0000-000000000004"),
        OTHER_STUDENT_ID,
        OTHER_INTERNSHIP_ID,
        OTHER_STUDENT_APP_USER);

    insertEvaluation(
        UUID.fromString("99999999-0000-0000-0000-000000000001"),
        STUDENT_ID,
        INTERNSHIP_ID,
        SUPERVISOR_ID,
        4, 5, 4, 4, 5,
        88,
        "SUBMITTED");
    insertEvaluation(
        UUID.fromString("99999999-0000-0000-0000-000000000002"),
        STUDENT_ID,
        INTERNSHIP_ID,
        SUPERVISOR_ID,
        2, 2, 2, 2, 2,
        40,
        "DRAFT");
    insertEvaluation(
        UUID.fromString("99999999-0000-0000-0000-000000000003"),
        OTHER_STUDENT_ID,
        OTHER_INTERNSHIP_ID,
        OTHER_SUPERVISOR_ID,
        3, 3, 3, 3, 3,
        60,
        "SUBMITTED");
  }

  private void insertEvaluation(
      UUID id,
      UUID studentId,
      UUID internshipId,
      UUID supervisorId,
      int technicalSkills,
      int communication,
      int teamwork,
      int problemSolving,
      int professionalism,
      int overall,
      String status) {
    jdbcTemplate.update(
        "INSERT INTO evaluation (id, student_id, internship_id, supervisor_id, technical_skills, "
            + "communication, teamwork, problem_solving, professionalism, overall_score, status, "
            + "created_by) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        id,
        studentId,
        internshipId,
        supervisorId,
        technicalSkills,
        communication,
        teamwork,
        problemSolving,
        professionalism,
        overall,
        status,
        supervisorId);
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

  private JsonNode data(String path, RequestPostProcessor postProcessor) throws Exception {
    return objectMapper.readTree(
        mockMvc
            .perform(get(path).with(postProcessor))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString())
        .path("data");
  }

  @Test
  void anonymousCannotReadReports() throws Exception {
    mockMvc.perform(get("/reports/internships")).andExpect(status().isUnauthorized());
  }

  @Test
  void studentCanReadOwnScopedReports() throws Exception {
    JsonNode internships =
        data("/reports/internships", auth(STUDENT_APP_USER.toString(), "STUDENT"));
    assertThat(internships.path("totalInternships").asLong()).isEqualTo(1);
    assertThat(internships.path("statusCounts").path("APPROVED").asLong()).isEqualTo(1);

    JsonNode activities = data("/reports/activities", auth(STUDENT_APP_USER.toString(), "STUDENT"));
    assertThat(activities.path("totalActivities").asLong()).isEqualTo(3);
    assertThat(activities.path("totalHours").asDouble()).isCloseTo(6.0, within(0.001));

    JsonNode evaluations = data("/reports/evaluations", auth(STUDENT_APP_USER.toString(), "STUDENT"));
    assertThat(evaluations.path("totalEvaluations").asLong()).isEqualTo(2);
    assertThat(evaluations.path("submittedEvaluations").asLong()).isEqualTo(1);
  }

  @Test
  void studentCannotRequestAnotherStudentsActivityReport() throws Exception {
    mockMvc
        .perform(
            get("/reports/activities")
                .param("studentId", OTHER_STUDENT_ID.toString())
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminInternshipReportAggregatesAll() throws Exception {
    String json =
        mockMvc
            .perform(get("/reports/internships").with(auth("admin", "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("INTERNSHIP"))
            .andExpect(jsonPath("$.data.totalInternships").value(2))
            .andExpect(jsonPath("$.data.statusCounts.APPROVED").value(1))
            .andExpect(jsonPath("$.data.statusCounts.REJECTED").value(1))
            .andExpect(jsonPath("$.data.placementRate").value(0.5))
            .andExpect(jsonPath("$.data.avgDurationDays").value(31.0))
            .andExpect(jsonPath("$.data.byProgram.length()").value(2))
            .andExpect(jsonPath("$.data.byProgram[0].program").value("CS"))
            .andExpect(jsonPath("$.data.byProgram[0].internships").value(1))
            .andExpect(jsonPath("$.data.byProgram[0].placementRate").value(1.0))
            .andExpect(jsonPath("$.data.byProgram[1].program").value("EE"))
            .andExpect(jsonPath("$.data.byProgram[1].placementRate").value(0.0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(objectMapper.readTree(json).path("data").has("generatedAt")).isTrue();
  }

  @Test
  void supervisorInternshipReportIsScopedToAssignedStudents() throws Exception {
    mockMvc
        .perform(get("/reports/internships").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalInternships").value(1))
        .andExpect(jsonPath("$.data.statusCounts.APPROVED").value(1))
        .andExpect(jsonPath("$.data.placementRate").value(1.0))
        .andExpect(jsonPath("$.data.byProgram[0].program").value("CS"));
  }

  @Test
  void adminActivityReportAggregatesAll() throws Exception {
    JsonNode data = data("/reports/activities", auth("admin", "ADMIN"));
    assertThat(data.path("type").asText()).isEqualTo("ACTIVITY");
    assertThat(data.path("totalActivities").asLong()).isEqualTo(4);
    assertThat(data.path("statusCounts").path("APPROVED").asLong()).isEqualTo(1);
    assertThat(data.path("statusCounts").path("SUBMITTED").asLong()).isEqualTo(2);
    assertThat(data.path("statusCounts").path("DRAFT").asLong()).isEqualTo(1);
    assertThat(data.path("approvalRate").asDouble()).isCloseTo(1.0 / 3, within(0.001));
    assertThat(data.path("pendingReviews").asLong()).isEqualTo(2);
    assertThat(data.path("totalHours").asDouble()).isCloseTo(10.0, within(0.001));
    assertThat(data.path("studentId").isNull()).isTrue();
  }

  @Test
  void adminActivityReportPerStudent() throws Exception {
    JsonNode data =
        data(
            "/reports/activities?studentId=" + STUDENT_ID,
            auth("admin", "ADMIN"));
    assertThat(data.path("totalActivities").asLong()).isEqualTo(3);
    assertThat(data.path("studentId").asText()).isEqualTo(STUDENT_ID.toString());
    assertThat(data.path("totalHours").asDouble()).isCloseTo(6.0, within(0.001));
  }

  @Test
  void supervisorActivityReportScopedToAssignedStudents() throws Exception {
    JsonNode data =
        data("/reports/activities", auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"));
    assertThat(data.path("totalActivities").asLong()).isEqualTo(3);
    assertThat(data.path("totalHours").asDouble()).isCloseTo(6.0, within(0.001));
  }

  @Test
  void supervisorCannotRequestActivityReportForUnassignedStudent() throws Exception {
    mockMvc
        .perform(
            get("/reports/activities")
                .param("studentId", OTHER_STUDENT_ID.toString())
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminActivityReportForUnknownStudentIsNotFound() throws Exception {
    mockMvc
        .perform(
            get("/reports/activities")
                .param("studentId", UUID.randomUUID().toString())
                .with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_PROFILE_NOT_FOUND"));
  }

  @Test
  void adminEvaluationReportCountsOnlySubmitted() throws Exception {
    String json =
        mockMvc
            .perform(get("/reports/evaluations").with(auth("admin", "ADMIN")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.type").value("EVALUATION"))
            .andExpect(jsonPath("$.data.totalEvaluations").value(3))
            .andExpect(jsonPath("$.data.submittedEvaluations").value(2))
            .andExpect(jsonPath("$.data.submissionRate").value(round(2.0 / 3)))
            .andExpect(jsonPath("$.data.dimensionAverages.technicalSkills").value(3.5))
            .andExpect(jsonPath("$.data.dimensionAverages.communication").value(4.0))
            .andExpect(jsonPath("$.data.averageOverallScore").value(74.0))
            .andExpect(jsonPath("$.data.scoreDistribution['80-89']").value(1))
            .andExpect(jsonPath("$.data.scoreDistribution['60-69']").value(1))
            .andExpect(jsonPath("$.data.scoreDistribution['<60']").value(0))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(objectMapper.readTree(json).path("data").has("generatedAt")).isTrue();
  }

  @Test
  void supervisorEvaluationReportScopedToOwnEvaluations() throws Exception {
    JsonNode data =
        data("/reports/evaluations", auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"));
    assertThat(data.path("totalEvaluations").asLong()).isEqualTo(2);
    assertThat(data.path("submittedEvaluations").asLong()).isEqualTo(1);
    assertThat(data.path("dimensionAverages").path("technicalSkills").asDouble())
        .isCloseTo(4.0, within(0.001));
    assertThat(data.path("averageOverallScore").asDouble()).isCloseTo(88.0, within(0.001));
  }

  @Test
  void facultyInternshipReportScopedToAcademicStudents() throws Exception {
    JsonNode data =
        data("/reports/internships", auth(FACULTY_APP_USER.toString(), "FACULTY"));
    assertThat(data.path("totalInternships").asLong()).isEqualTo(1);
    assertThat(data.path("statusCounts").path("APPROVED").asLong()).isEqualTo(1);
    assertThat(data.path("byProgram").size()).isEqualTo(1);
    assertThat(data.path("byProgram").get(0).path("program").asText()).isEqualTo("CS");
  }

  @Test
  void facultyActivityReportScopedToAssignedStudents() throws Exception {
    JsonNode data =
        data("/reports/activities", auth(FACULTY_APP_USER.toString(), "FACULTY"));
    assertThat(data.path("totalActivities").asLong()).isEqualTo(3);
    assertThat(data.path("totalHours").asDouble()).isCloseTo(6.0, within(0.001));
  }

  @Test
  void facultyEvaluationReportScopedToAssignedStudents() throws Exception {
    JsonNode data =
        data("/reports/evaluations", auth(FACULTY_APP_USER.toString(), "FACULTY"));
    assertThat(data.path("totalEvaluations").asLong()).isEqualTo(2);
    assertThat(data.path("submittedEvaluations").asLong()).isEqualTo(1);
    assertThat(data.path("averageOverallScore").asDouble()).isCloseTo(88.0, within(0.001));
  }

  @Test
  void facultyExportsScopedInternshipPdf() throws Exception {
    byte[] bytes =
        exportPdf("/reports/internships/export", auth(FACULTY_APP_USER.toString(), "FACULTY"));

    String text = pdfText(bytes);
    assertThat(text).contains("Internship Report");
    assertThat(text).contains("CS");
    assertThat(text).doesNotContain("EE");
  }

  @Test
  void adminExportsInternshipReportAsPdf() throws Exception {
    byte[] bytes = exportPdf("/reports/internships/export", auth("admin", "ADMIN"));

    String text = pdfText(bytes);
    assertThat(text).contains("Internship Report");
    assertThat(text).contains("Placement rate");
    assertThat(text).contains("By program");
    assertThat(text).contains("CS");
    assertThat(text).contains("EE");
  }

  @Test
  void supervisorExportsScopedInternshipPdf() throws Exception {
    byte[] bytes =
        exportPdf(
            "/reports/internships/export", auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"));

    String text = pdfText(bytes);
    assertThat(text).contains("Internship Report");
    assertThat(text).contains("CS");
    assertThat(text).doesNotContain("EE");
  }

  @Test
  void adminExportsActivityPdfScopedToStudent() throws Exception {
    byte[] bytes =
        exportPdf(
            "/reports/activities/export?studentId=" + STUDENT_ID, auth("admin", "ADMIN"));

    String text = pdfText(bytes);
    assertThat(text).contains("Activity Report");
    assertThat(text).contains("Total activities");
    assertThat(text).contains("Status distribution");
  }

  @Test
  void adminExportsEvaluationReportAsPdf() throws Exception {
    byte[] bytes = exportPdf("/reports/evaluations/export", auth("admin", "ADMIN"));

    String text = pdfText(bytes);
    assertThat(text).contains("Evaluation Report");
    assertThat(text).contains("Dimension averages");
    assertThat(text).contains("Technical Skills");
    assertThat(text).contains("Score distribution");
    assertThat(text).contains("90-100");
  }

  @Test
  void studentCanExportOwnInternshipPdf() throws Exception {
    byte[] bytes =
        exportPdf("/reports/internships/export", auth(STUDENT_APP_USER.toString(), "STUDENT"));

    String text = pdfText(bytes);
    assertThat(text).contains("Internship Report");
    assertThat(text).contains("CS");
    assertThat(text).doesNotContain("EE");
  }

  @Test
  void unsupportedExportFormatIsRejected() throws Exception {
    mockMvc
        .perform(get("/reports/internships/export").param("format", "csv").with(auth("admin", "ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_FORMAT"));
  }

  @Test
  void adminExportsInternshipReportAsXlsx() throws Exception {
    byte[] bytes = exportXlsx("/reports/internships/export", auth("admin", "ADMIN"));

    List<String> text = sheetText(bytes);
    assertThat(text).contains("Internship Report", "Total internships", "Placement rate");
    assertThat(text).contains("Program", "CS", "EE");
  }

  @Test
  void supervisorExportsScopedInternshipXlsx() throws Exception {
    byte[] bytes =
        exportXlsx(
            "/reports/internships/export", auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"));

    List<String> text = sheetText(bytes);
    assertThat(text).contains("CS");
    assertThat(text).doesNotContain("EE");
  }

  @Test
  void adminExportsActivityAndEvaluationReportsAsXlsx() throws Exception {
    List<String> activity =
        sheetText(
            exportXlsx(
                "/reports/activities/export?studentId=" + STUDENT_ID, auth("admin", "ADMIN")));
    assertThat(activity).contains("Activity Report", "Total activities", "Status");

    List<String> evaluation =
        sheetText(exportXlsx("/reports/evaluations/export", auth("admin", "ADMIN")));
    assertThat(evaluation)
        .contains(
            "Evaluation Report",
            "Dimension",
            "Technical Skills",
            "Score range",
            "90-100");
  }

  @Test
  void unknownReportTypeIsRejected() throws Exception {
    mockMvc
        .perform(get("/reports/students/export").with(auth("admin", "ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("UNKNOWN_REPORT_TYPE"));
  }

  private byte[] exportPdf(String path, RequestPostProcessor postProcessor) throws Exception {
    return mockMvc
        .perform(get(path).with(postProcessor))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_PDF))
        .andExpect(
            header()
                .string(
                    HttpHeaders.CONTENT_DISPOSITION,
                    containsString("attachment; filename=\"sitms-")))
        .andReturn()
        .getResponse()
        .getContentAsByteArray();
  }

  private static String pdfText(byte[] bytes) throws Exception {
    assertThat(bytes.length).isGreaterThan(100);
    assertThat(new String(bytes, StandardCharsets.ISO_8859_1)).startsWith("%PDF-");
    return new PdfTextExtractor(new PdfReader(bytes)).getTextFromPage(1);
  }

  private static final String XLSX_MEDIA_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

  private byte[] exportXlsx(String path, RequestPostProcessor postProcessor) throws Exception {
    String url = path.contains("?") ? path + "&format=xlsx" : path + "?format=xlsx";
    return mockMvc
        .perform(get(url).with(postProcessor))
        .andExpect(status().isOk())
        .andExpect(content().contentType(XLSX_MEDIA_TYPE))
        .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString(".xlsx")))
        .andReturn()
        .getResponse()
        .getContentAsByteArray();
  }

  private static List<String> sheetText(byte[] bytes) throws Exception {
    assertThat(bytes.length).isGreaterThan(100);
    assertThat(new String(bytes, StandardCharsets.ISO_8859_1)).startsWith("PK");
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      List<String> text = new ArrayList<>();
      for (Sheet sheet : workbook) {
        for (Row row : sheet) {
          for (Cell cell : row) {
            if (cell.getCellType() == CellType.STRING) {
              text.add(cell.getStringCellValue());
            }
          }
        }
      }
      return text;
    }
  }

  private static double round(double value) {
    return BigDecimal.valueOf(value).setScale(3, RoundingMode.HALF_UP).doubleValue();
  }
}