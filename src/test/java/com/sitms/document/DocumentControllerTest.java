package com.sitms.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

/** Integration tests for {@link DocumentController} (SITMS-037): upload, download, validation. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class DocumentControllerTest {

  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID STUDENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID ORGANIZATION_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID OTHER_STUDENT_APP_USER = UUID.fromString("aaaaaaa1-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_STUDENT_ID = UUID.fromString("bbbbbbb1-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
  private static final UUID OTHER_SUPERVISOR_APP_USER =
      UUID.fromString("aaaaaaa2-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
  private static final UUID OTHER_SUPERVISOR_ID = UUID.fromString("bbbbbbb2-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private DocumentRepository documentRepository;
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
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        INTERNSHIP_ID,
        STUDENT_ID,
        ORGANIZATION_ID,
        SUPERVISOR_ID,
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
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Other Internship', ?)",
        UUID.randomUUID(),
        OTHER_STUDENT_ID,
        ORGANIZATION_ID,
        OTHER_SUPERVISOR_ID,
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

  private static MockMultipartFile pdfFile(String name) {
    return new MockMultipartFile("file", name, "application/pdf", new byte[] {1, 2, 3, 4});
  }

  private UUID createDocument() throws Exception {
    return createDocumentFor(STUDENT_APP_USER);
  }

  private UUID createDocumentFor(UUID appUserId) throws Exception {
    String json =
        mockMvc
            .perform(
                multipart("/documents")
                    .file(pdfFile("report.pdf"))
                    .param("type", "REPORT")
                    .with(auth(appUserId.toString(), "STUDENT")))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(
        com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
            .readTree(json).path("data").path("id").asText());
  }

  @Test
  void anonymousCannotUpload() throws Exception {
    mockMvc.perform(multipart("/documents").file(pdfFile("a.pdf"))).andExpect(status().isUnauthorized());
  }

  @Test
  void studentUploadsOwnDocumentAsPending() throws Exception {
    mockMvc
        .perform(
            multipart("/documents")
                .file(pdfFile("report.pdf"))
                .param("type", "REPORT")
                .param("internshipId", INTERNSHIP_ID.toString())
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("PENDING"))
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()))
        .andExpect(jsonPath("$.data.fileName").value("report.pdf"))
        .andExpect(jsonPath("$.data.type").value("REPORT"))
        .andExpect(jsonPath("$.data.size").value(4));

    var docs = documentRepository.findByStudentId(STUDENT_ID, org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
    assertThat(docs).hasSize(1);
  }

  @Test
  void studentWithoutProfileCannotUpload() throws Exception {
    var noProfile = UUID.fromString("99999999-9999-9999-9999-999999999999");
    mockMvc
        .perform(
            multipart("/documents")
                .file(pdfFile("report.pdf"))
                .with(auth(noProfile.toString(), "STUDENT")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_PROFILE_NOT_FOUND"));
  }

  @Test
  void adminUploadsOnBehalfOfStudent() throws Exception {
    mockMvc
        .perform(
            multipart("/documents")
                .file(pdfFile("evidence.png"))
                .param("type", "EVIDENCE")
                .param("studentId", STUDENT_ID.toString())
                .with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.studentId").value(STUDENT_ID.toString()));

    var docs = documentRepository.findByStudentId(STUDENT_ID, org.springframework.data.domain.PageRequest.of(0, 10)).getContent();
    assertThat(docs).hasSize(1);
  }

  @Test
  void adminUploadWithoutStudentIdRejected() throws Exception {
    mockMvc
        .perform(multipart("/documents").file(pdfFile("a.pdf")).with(auth("admin", "ADMIN")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("STUDENT_PROFILE_NOT_FOUND"));
  }

  @Test
  void rejectsDisallowedFileType() throws Exception {
    mockMvc
        .perform(
            multipart("/documents")
                .file(new MockMultipartFile("file", "virus.exe", "application/x-msdownload", new byte[] {1}))
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_FILE_TYPE"));
  }

  @Test
  void rejectsEmptyFile() throws Exception {
    mockMvc
        .perform(
            multipart("/documents")
                .file(new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]))
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("EMPTY_FILE"));
  }

  @Test
  void rejectsOversizedFile() throws Exception {
    byte[] big = new byte[(int) (DocumentService.MAX_FILE_SIZE + 1)];
    big[0] = 1;
    mockMvc
        .perform(
            multipart("/documents")
                .file(new MockMultipartFile("file", "huge.pdf", "application/pdf", big))
                .with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("FILE_TOO_LARGE"));
  }

  @Test
  void ownerStudentDownloadsOwnDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(get("/documents/{id}/download", id).with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"))
        .andExpect(header().exists("Content-Disposition"))
        .andExpect(result -> assertThat(result.getResponse().getContentAsByteArray()).isEqualTo(new byte[] {1, 2, 3, 4}));
  }

  @Test
  void otherStudentCannotDownload() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(get("/documents/{id}/download", id).with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void assignedSupervisorCanDownload() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(get("/documents/{id}/download", id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
  }

  @Test
  void adminCanDownload() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(get("/documents/{id}/download", id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "application/pdf"));
  }

@Test
  void unknownDocumentReturns404() throws Exception {
    mockMvc
        .perform(get("/documents/{id}/download", UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value("DOCUMENT_NOT_FOUND"));
  }

  @Test
  void supervisorReviewsAssignedStudentsPendingDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(
            post("/documents/{id}/review", id)
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED", "feedback", "Valid"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("APPROVED"))
        .andExpect(jsonPath("$.data.reviewerId").value(SUPERVISOR_ID.toString()))
        .andExpect(jsonPath("$.data.reviewNote").value("Valid"));
  }

  @Test
  void supervisorCannotReviewUnassignedStudentsDocument() throws Exception {
    UUID id = createDocumentFor(OTHER_STUDENT_APP_USER);
    mockMvc
        .perform(
            post("/documents/{id}/review", id)
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminReviewsAnyPendingDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(
            post("/documents/{id}/review", id)
                .with(auth("admin", "ADMIN"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("decision", "REJECTED", "feedback", "No."))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.status").value("REJECTED"));
  }

  @Test
  void cannotReviewNonPendingDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(
            post("/documents/{id}/review", id)
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED"))))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            post("/documents/{id}/review", id)
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("decision", "REJECTED"))))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"));
  }

  @Test
  void supervisorSeesOnlyAssignedPendingInReviewQueue() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(get("/documents/review-queue").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(id.toString()));
  }

  @Test
  void adminSeesAllPendingInReviewQueue() throws Exception {
    createDocument();
    mockMvc
        .perform(get("/documents/review-queue").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void studentCannotAccessReviewQueue() throws Exception {
    mockMvc
        .perform(get("/documents/review-queue").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
  }

  @Test
  void studentSeesOnlyOwnDocumentsInList() throws Exception {
    UUID id = createDocument();
    createDocument();
    mockMvc
        .perform(get("/documents").with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(2))
        .andExpect(jsonPath("$.data.items[0].id").value(id.toString()));
  }

  @Test
  void adminListsAllDocuments() throws Exception {
    createDocument();
    mockMvc
        .perform(get("/documents").with(auth("admin", "ADMIN")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1));
  }

  @Test
  void supervisorSeesOnlyAssignedStudentsDocumentsInList() throws Exception {
    UUID assignedId = createDocument();
    createDocumentFor(OTHER_STUDENT_APP_USER);

    mockMvc
        .perform(get("/documents").with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(assignedId.toString()));
  }

  @Test
  void supervisorCanViewAnotherSupervisorsAssignedStudentDocuments() throws Exception {
    createDocument();
    UUID otherId = createDocumentFor(OTHER_STUDENT_APP_USER);

    mockMvc
        .perform(
            get("/documents")
                .with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR"))
                .param("supervisorId", OTHER_SUPERVISOR_ID.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.totalElements").value(1))
        .andExpect(jsonPath("$.data.items[0].id").value(otherId.toString()));
  }

  @Test
  void anonymousCannotDeleteDocument() throws Exception {
    UUID id = createDocument();
    mockMvc.perform(delete("/documents/{id}", id)).andExpect(status().isUnauthorized());
  }

  @Test
  void ownerStudentDeletesOwnDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(delete("/documents/{id}", id).with(auth(STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isOk());
    assertThat(documentRepository.findById(id)).isEmpty();
  }

  @Test
  void otherStudentCannotDeleteDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(delete("/documents/{id}", id).with(auth(OTHER_STUDENT_APP_USER.toString(), "STUDENT")))
        .andExpect(status().isForbidden());
    assertThat(documentRepository.findById(id)).isPresent();
  }

  @Test
  void assignedSupervisorDeletesDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(delete("/documents/{id}", id).with(auth(SUPERVISOR_APP_USER.toString(), "SUPERVISOR")))
        .andExpect(status().isOk());
    assertThat(documentRepository.findById(id)).isEmpty();
  }

  @Test
  void adminDeletesAnyDocument() throws Exception {
    UUID id = createDocument();
    mockMvc
        .perform(delete("/documents/{id}", id).with(auth("admin", "ADMIN")))
        .andExpect(status().isOk());
    assertThat(documentRepository.findById(id)).isEmpty();
  }

  @Test
  void deleteUnknownDocumentReturnsNotFound() throws Exception {
    mockMvc
        .perform(delete("/documents/{id}", UUID.randomUUID()).with(auth("admin", "ADMIN")))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.errorCode").value(DocumentService.DOCUMENT_NOT_FOUND));
  }
}