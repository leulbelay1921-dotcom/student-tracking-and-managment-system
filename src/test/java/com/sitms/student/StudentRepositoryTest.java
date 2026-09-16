package com.sitms.student;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.config.JpaAuditingConfig;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository integration test for {@link Student} against a real Postgres (SITMS-006). Exercises
 * persist/find, audit-column population, and unique constraints.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class StudentRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Autowired private StudentRepository studentRepository;
  @Autowired private DataSource dataSource;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persistsAndRetrievesStudent() {
    UUID appUserId = insertAppUser("alice");
    setAuditor(AUDITOR_ID);

    Student saved = studentRepository.save(newStudent(appUserId, "S1001", "Alice", "CS", 2));

    Student found = studentRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getAppUserId()).isEqualTo(appUserId);
    assertThat(found.getAppUserUsername()).isEqualTo("student-S1001");
    assertThat(found.getStudentNo()).isEqualTo("S1001");
    assertThat(found.getProgram()).isEqualTo("CS");
    assertThat(found.getLevel()).isEqualTo(2);
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    UUID appUserId = insertAppUser("bob");
    setAuditor(AUDITOR_ID);

    Student saved = studentRepository.save(newStudent(appUserId, "S1002", "Bob", "EE", 1));
    studentRepository.flush();

    Student found = studentRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByAppUserIdAndStudentNo() {
    UUID appUserId = insertAppUser("carol");
    studentRepository.save(newStudent(appUserId, "S1003", "Carol", "ME", 3));

    assertThat(studentRepository.findByAppUserId(appUserId)).isPresent();
    assertThat(studentRepository.findByStudentNo("S1003")).isPresent();
    assertThat(studentRepository.existsByStudentNo("S1003")).isTrue();
    assertThat(studentRepository.existsByStudentNo("NOPE")).isFalse();
  }

  @Test
  void findsByProgramPaged() {
    studentRepository.save(newStudent(insertAppUser("dave"), "S2001", "Dave", "CS", 1));
    studentRepository.save(newStudent(insertAppUser("eve"), "S2002", "Eve", "CS", 2));
    studentRepository.save(newStudent(insertAppUser("frank"), "S2003", "Frank", "ME", 1));

    Page<Student> cs = studentRepository.findByProgram("CS", PageRequest.of(0, 1));
    assertThat(cs.getTotalElements()).isEqualTo(2);
    assertThat(cs.getContent()).hasSize(1);
  }

  @Test
  void rejectsDuplicateStudentNo() {
    studentRepository.save(newStudent(insertAppUser("grace"), "S3001", "Grace", "CS", 1));
    studentRepository.flush();

    org.junit.jupiter.api.Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          studentRepository.save(newStudent(insertAppUser("heidi"), "S3001", "Heidi", "CS", 1));
          studentRepository.flush();
        });
  }

  @Test
  void rejectsDuplicateAppUserId() {
    UUID appUserId = insertAppUser("ivan");
    studentRepository.save(newStudent(appUserId, "S4001", "Ivan", "CS", 1));
    studentRepository.flush();

    org.junit.jupiter.api.Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          studentRepository.save(newStudent(appUserId, "S4002", "Judy", "CS", 1));
          studentRepository.flush();
        });
  }

  private static Student newStudent(
      UUID appUserId, String no, String name, String program, int level) {
    return Student.builder()
        .appUserId(appUserId)
        .appUserUsername("student-" + no)
        .studentNo(no)
        .fullName(name)
        .program(program)
        .level(level)
        .build();
  }

  private UUID insertAppUser(String username) {
    UUID id = UUID.randomUUID();
    try (var connection = dataSource.getConnection();
        var statement =
            connection.prepareStatement(
                "INSERT INTO app_user (id, username, email, display_name, role, active, created_by)"
                    + " VALUES (?, ?, ?, ?, 'STUDENT', TRUE, ?)")) {
      statement.setObject(1, id);
      statement.setString(2, username);
      statement.setString(3, username + "@example.com");
      statement.setString(4, username);
      statement.setObject(5, AUDITOR_ID);
      statement.executeUpdate();
      return id;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to insert app_user fixture", e);
    }
  }

  private void setAuditor(UUID id) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, id.toString())
            .claim("realm_access", java.util.Map.of("roles", List.of("STUDENT")))
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new JwtAuthenticationToken(jwt, List.of()));
  }
}
