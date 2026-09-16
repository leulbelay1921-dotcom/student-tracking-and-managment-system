package com.sitms.supervisor;

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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository integration test for {@link Supervisor} against a real Postgres (SITMS-014):
 * persist/find, audit columns, FKs to app_user/organization, and unique constraints.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class SupervisorRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID APP_USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID ORGANIZATION_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");

  @Autowired private SupervisorRepository supervisorRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void seedParents() {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, created_by) "
            + "VALUES (?, 'supervisor', 'sup@example.com', 'Test Supervisor', 'SUPERVISOR', ?)",
        APP_USER_ID,
        AUDITOR_ID);
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Acme Corp', ?)",
        ORGANIZATION_ID,
        AUDITOR_ID);
  }

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persistsAndRetrievesSupervisor() {
    setAuditor(AUDITOR_ID);
    Supervisor saved =
        supervisorRepository.save(
            Supervisor.builder()
                .appUserId(APP_USER_ID)
                .organizationId(ORGANIZATION_ID)
                .employeeNo("EMP-001")
                .fullName("Jane Doe")
                .jobTitle("Engineering Lead")
                .department("R&D")
                .build());

    Supervisor found = supervisorRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getFullName()).isEqualTo("Jane Doe");
    assertThat(found.getJobTitle()).isEqualTo("Engineering Lead");
    assertThat(found.getDepartment()).isEqualTo("R&D");
    assertThat(found.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
    assertThat(found.isActive()).isTrue();
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Supervisor saved = supervisorRepository.save(newSupervisor("EMP-002"));
    supervisorRepository.flush();

    Supervisor found = supervisorRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByEmployeeNoAndAppUser() {
    supervisorRepository.save(newSupervisor("EMP-003"));

    assertThat(supervisorRepository.findByEmployeeNo("EMP-003")).isPresent();
    assertThat(supervisorRepository.existsByEmployeeNo("EMP-003")).isTrue();
    assertThat(supervisorRepository.findByAppUserId(APP_USER_ID)).isPresent();
  }

  @Test
  void findsByOrganizationAndActivePaged() {
    UUID otherOrg = UUID.fromString("55555555-5555-5555-5555-555555555555");
    jdbcTemplate.update(
        "INSERT INTO organization (id, name, created_by) VALUES (?, 'Globex', ?)", otherOrg, AUDITOR_ID);

    supervisorRepository.save(newSupervisor("EMP-004"));
    supervisorRepository.save(atOrg(UUID.randomUUID(), otherOrg, "EMP-005", false));

    Page<Supervisor> acme =
        supervisorRepository.findByOrganizationId(ORGANIZATION_ID, PageRequest.of(0, 10));
    assertThat(acme.getTotalElements()).isEqualTo(1);

    Page<Supervisor> active = supervisorRepository.findByActive(true, PageRequest.of(0, 10));
    assertThat(active.getTotalElements()).isEqualTo(1);
  }

  @Test
  void rejectsDuplicateEmployeeNo() {
    supervisorRepository.save(atOrg(UUID.randomUUID(), ORGANIZATION_ID, "EMP-100", true));
    supervisorRepository.flush();

    org.junit.jupiter.api.Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          supervisorRepository.save(atOrg(UUID.randomUUID(), ORGANIZATION_ID, "EMP-100", true));
          supervisorRepository.flush();
        });
  }

  @Test
  void rejectsUnknownOrganization() {
    org.junit.jupiter.api.Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          supervisorRepository.save(
              Supervisor.builder()
                  .appUserId(APP_USER_ID)
                  .organizationId(UUID.randomUUID())
                  .employeeNo("EMP-101")
                  .fullName("No Org")
                  .build());
          supervisorRepository.flush();
        });
  }

  private static Supervisor newSupervisor(String employeeNo) {
    return Supervisor.builder()
        .appUserId(APP_USER_ID)
        .organizationId(ORGANIZATION_ID)
        .employeeNo(employeeNo)
        .fullName("Test Supervisor")
        .build();
  }

  private Supervisor atOrg(UUID appUserId, UUID organizationId, String employeeNo, boolean active) {
    jdbcTemplate.update(
        "INSERT INTO app_user (id, username, email, display_name, role, created_by) "
            + "VALUES (?, ?, ?, ?, 'SUPERVISOR', ?)",
        appUserId,
        employeeNo,
        employeeNo + "@example.com",
        employeeNo,
        AUDITOR_ID);
    return Supervisor.builder()
        .appUserId(appUserId)
        .organizationId(organizationId)
        .employeeNo(employeeNo)
        .fullName("Test Supervisor")
        .active(active)
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
