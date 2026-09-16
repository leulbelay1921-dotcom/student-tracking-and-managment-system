package com.sitms.organization;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.config.JpaAuditingConfig;
import java.util.List;
import java.util.UUID;
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
 * Repository integration test for {@link Organization} against a real Postgres (SITMS-011):
 * persist/find, audit columns, and unique constraint.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class OrganizationRepositoryTest {

  private static final UUID AUDITOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

  @Autowired private OrganizationRepository organizationRepository;

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void persistsAndRetrievesOrganization() {
    setAuditor(AUDITOR_ID);
    Organization saved =
        organizationRepository.save(
            Organization.builder()
                .name("Acme Corp")
                .industry("Technology")
                .contactEmail("hr@acme.com")
                .website("https://acme.com")
                .build());

    Organization found = organizationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getName()).isEqualTo("Acme Corp");
    assertThat(found.getIndustry()).isEqualTo("Technology");
    assertThat(found.getContactEmail()).isEqualTo("hr@acme.com");
    assertThat(found.isActive()).isTrue();
  }

  @Test
  void populatesAuditColumnsOnPersist() {
    setAuditor(AUDITOR_ID);
    Organization saved = organizationRepository.save(newOrg("Globex"));
    organizationRepository.flush();

    Organization found = organizationRepository.findById(saved.getId()).orElseThrow();
    assertThat(found.getCreatedBy()).isEqualTo(AUDITOR_ID);
    assertThat(found.getCreatedAt()).isNotNull();
    assertThat(found.getUpdatedAt()).isNotNull();
    assertThat(found.getVersion()).isEqualTo(0);
  }

  @Test
  void findsByNameAndExists() {
    organizationRepository.save(newOrg("Initech"));

    assertThat(organizationRepository.findByName("Initech")).isPresent();
    assertThat(organizationRepository.existsByName("Initech")).isTrue();
    assertThat(organizationRepository.existsByName("Missing")).isFalse();
  }

  @Test
  void findsByActiveAndIndustryPaged() {
    organizationRepository.save(
        Organization.builder().name("A").industry("Tech").active(true).build());
    organizationRepository.save(
        Organization.builder().name("B").industry("Tech").active(true).build());
    organizationRepository.save(
        Organization.builder().name("C").industry("Health").active(false).build());

    Page<Organization> tech = organizationRepository.findByIndustry("Tech", PageRequest.of(0, 1));
    assertThat(tech.getTotalElements()).isEqualTo(2);
    assertThat(tech.getContent()).hasSize(1);

    Page<Organization> active = organizationRepository.findByActive(true, PageRequest.of(0, 10));
    assertThat(active.getTotalElements()).isEqualTo(2);
  }

  @Test
  void rejectsDuplicateName() {
    organizationRepository.save(newOrg("Umbrella"));
    organizationRepository.flush();

    org.junit.jupiter.api.Assertions.assertThrows(
        DataIntegrityViolationException.class,
        () -> {
          organizationRepository.save(newOrg("Umbrella"));
          organizationRepository.flush();
        });
  }

  private static Organization newOrg(String name) {
    return Organization.builder().name(name).build();
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
