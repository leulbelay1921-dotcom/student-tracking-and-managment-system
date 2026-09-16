package com.sitms.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository integration test for {@link Document} against a real Postgres (SITMS-036):
 * persist/find, audit columns, student/status/supervisor queries, and the review mutator.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class DocumentRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_STUDENT_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID SUPERVISOR_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

  @Autowired private DocumentRepository documentRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedParents() {
    seedAppUser(STUDENT_ID, "student1");
    seedAppUser(OTHER_STUDENT_ID, "student2");
    seedAppUser(SUPERVISOR_ID, "supervisor1");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-1', 'Student One', 'CS', ?)",
        STUDENT_ID,
        STUDENT_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO student (id, app_user_id, student_no, full_name, program, created_by) "
            + "VALUES (?, ?, 'S-2', 'Student Two', 'CS', ?)",
        OTHER_STUDENT_ID,
        OTHER_STUDENT_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO supervisor (id, app_user_id, organization_id, employee_no, full_name, created_by) "
            + "VALUES (?, ?, ?, 'EMP-1', 'Sup One', ?)",
        SUPERVISOR_ID,
        SUPERVISOR_ID,
        ORGANIZATION_ID,
        AUDITOR_ID);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  private void seedAppUser(UUID id, String username) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, active, created_by) "
            + "VALUES (?, ?, ?, ?, 'ADMIN', TRUE, ?)",
        id,
        username,
        username + "@example.com",
        username,
        AUDITOR_ID);
  }

  private void seedInternship(UUID studentId, UUID supervisorId) {
    jdbcTemplate.update(
        "INSERT INTO internship (id, student_id, organization_id, supervisor_id, title, created_by) "
            + "VALUES (?, ?, ?, ?, 'Internship', ?)",
        UUID.randomUUID(),
        studentId,
        ORGANIZATION_ID,
        supervisorId,
        AUDITOR_ID);
  }

  @Test
  void persistsAndRetrievesDocument() {
    setAuditor(AUDITOR_ID);
    Document saved =
        documentRepository.save(
            newDocument(STUDENT_ID, DocumentStatus.PENDING, DocumentType.OFFER_LETTER));

    Document found = documentRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStudentId()).isEqualTo(STUDENT_ID);
    assertThat(found.getFileName()).isEqualTo("offer.pdf");
    assertThat(found.getContentType()).isEqualTo("application/pdf");
    assertThat(found.getSize()).isEqualTo(10);
    assertThat(found.getType()).isEqualTo(DocumentType.OFFER_LETTER);
    assertThat(found.getStatus()).isEqualTo(DocumentStatus.PENDING);
    assertThat(found.getContent()).isEqualTo(new byte[] {1, 2, 3});
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Document saved =
        documentRepository.save(
            newDocument(STUDENT_ID, DocumentStatus.PENDING, DocumentType.REPORT));
    documentRepository.flush();

    Document found = documentRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByStudentPaged() {
    setAuditor(AUDITOR_ID);
    documentRepository.save(newDocument(STUDENT_ID, DocumentStatus.PENDING, DocumentType.REPORT));
    documentRepository.save(newDocument(STUDENT_ID, DocumentStatus.APPROVED, DocumentType.EVIDENCE));
    documentRepository.save(newDocument(OTHER_STUDENT_ID, DocumentStatus.PENDING, DocumentType.OTHER));

    Page<Document> page = documentRepository.findByStudentId(STUDENT_ID, PageRequest.of(0, 10));
    assertThat(page.getTotalElements()).isEqualTo(2);

    Page<Document> pending =
        documentRepository.findByStudentIdAndStatus(
            STUDENT_ID, DocumentStatus.PENDING, PageRequest.of(0, 10));
    assertThat(pending.getTotalElements()).isEqualTo(1);
  }

  @Test
  void findsByStatusPaged() {
    setAuditor(AUDITOR_ID);
    documentRepository.save(newDocument(STUDENT_ID, DocumentStatus.PENDING, DocumentType.REPORT));
    documentRepository.save(newDocument(OTHER_STUDENT_ID, DocumentStatus.APPROVED, DocumentType.REPORT));

    Page<Document> pending =
        documentRepository.findByStatus(DocumentStatus.PENDING, PageRequest.of(0, 10));
    assertThat(pending.getTotalElements()).isEqualTo(1);
  }

  @Test
  void findsPendingAssignedToSupervisor() {
    setAuditor(AUDITOR_ID);
    seedInternship(STUDENT_ID, SUPERVISOR_ID);
    documentRepository.save(newDocument(STUDENT_ID, DocumentStatus.PENDING, DocumentType.REPORT));
    documentRepository.save(newDocument(OTHER_STUDENT_ID, DocumentStatus.PENDING, DocumentType.REPORT));

    Page<Document> pending =
        documentRepository.findByStatusAndAssignedSupervisor(
            DocumentStatus.PENDING, SUPERVISOR_ID, PageRequest.of(0, 10));
    assertThat(pending.getTotalElements()).isEqualTo(1);
    assertThat(pending.getContent().get(0).getStudentId()).isEqualTo(STUDENT_ID);
  }

  @Test
  void updatesReviewDecision() {
    setAuditor(AUDITOR_ID);
    Document saved =
        documentRepository.saveAndFlush(
            newDocument(STUDENT_ID, DocumentStatus.PENDING, DocumentType.REPORT));

    saved.updateReview(SUPERVISOR_ID, DocumentStatus.APPROVED, "Looks good");
    documentRepository.saveAndFlush(saved);

    Document found = documentRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getStatus()).isEqualTo(DocumentStatus.APPROVED);
    assertThat(found.getReviewerId()).isEqualTo(SUPERVISOR_ID);
    assertThat(found.getReviewNote()).isEqualTo("Looks good");
  }

  private static Document newDocument(UUID studentId, DocumentStatus status, DocumentType type) {
    return Document.builder()
        .studentId(studentId)
        .fileName("offer.pdf")
        .contentType("application/pdf")
        .size(10)
        .type(type)
        .status(status)
        .content(new byte[] {1, 2, 3})
        .build();
  }

  private void setAuditor(UUID id) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, id.toString())
            .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}