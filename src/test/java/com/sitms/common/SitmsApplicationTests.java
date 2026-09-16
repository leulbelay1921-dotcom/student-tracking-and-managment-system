package com.sitms.common;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Boots the full application context against an embedded H2 database and verifies the context
 * loads successfully with Hibernate schema generation.
 */
@SpringBootTest
@ActiveProfiles("test")
class SitmsApplicationTests {

  @Test
  void contextLoads() {
    org.assertj.core.api.Assertions.assertThat(true).isTrue();
  }
}
