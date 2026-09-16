package com.sitms.common.security;

import com.sitms.common.config.SecurityContextAuditor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Resource-server security for the SITMS backend.
 *
 * <p>Requests are authorized via a JWT issued by Keycloak (SITMS-005). Realm roles such as
 * {@code ADMIN}, {@code STUDENT} and {@code SUPERVISOR} are mapped from {@code realm_access.roles}
 * (and {@code resource_access.<client>.roles}) into Spring authorities prefixed {@code ROLE_} so
 * that {@code hasRole(...)} / {@code @PreAuthorize("hasRole(...)")} work as expected.
 *
 * <p>The filter chain is intentionally route-based first (a few coarse rules for future feature
 * endpoints) and leans on {@code @EnableMethodSecurity} for fine-grained per-endpoint rules.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final String issuerUri;
  private final String webAppClientId;

  public SecurityConfig(
      @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
      @Value("${app.security.resource-server.client-id}") String webAppClientId) {
    this.issuerUri = issuerUri;
    this.webAppClientId = webAppClientId;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
                auth ->
                    auth.requestMatchers("/actuator/health/**", "/actuator/info/**")
                        .permitAll()
                    .requestMatchers(HttpMethod.GET, "/") // status probe (SITMS-003)
                        .permitAll()
                    .requestMatchers(
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/v3/api-docs")
                        .permitAll()
                    // Future feature endpoints. Nested admin/supervisor namespaces are role-gated
                    // up front; the base paths are handled per-endpoint via @PreAuthorize.
                    // Paths are context-relative (server.servlet.context-path=/api is stripped
                    // before these rules match in the running server).
                    .requestMatchers("/admin/**")
                        .hasRole("ADMIN")
                    .requestMatchers("/supervisor/**")
                        .hasAnyRole("ADMIN", "SUPERVISOR")
                    .requestMatchers("/students/**")
                        .hasAnyRole("ADMIN", "SUPERVISOR", "FACULTY", "STUDENT")
                    .anyRequest()
                        .authenticated())
        .oauth2ResourceServer(
            oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
    return http.build();
  }

  /**
   * Maps Keycloak roles into {@code ROLE_*} Spring authorities. Realm roles live under
   * {@code realm_access.roles}; client-level roles under {@code resource_access.<client>.roles}.
   */
  @Bean
  public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
    return new Converter<Jwt, AbstractAuthenticationToken>() {
      @Override
      public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities);
      }
    };
  }

  private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
    List<String> roles = new ArrayList<>();
    if (jwt.hasClaim("realm_access")) {
      roles.addAll(readRoles((Map<?, ?>) jwt.getClaimAsMap("realm_access")));
    }
    if (jwt.hasClaim("resource_access")) {
      Map<?, ?> resourceAccess = jwt.getClaimAsMap("resource_access");
      Object client = resourceAccess.get(webAppClientId);
      if (client instanceof Map<?, ?>) {
        roles.addAll(readRoles((Map<?, ?>) client));
      }
    }
    return roles.stream()
        .flatMap(role -> canonicalRoles(role).stream())
        .distinct()
        .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
        .toList();
  }

  private static List<String> canonicalRoles(String role) {
    if ("COMPANY_SUPERVISOR".equals(role)) {
      return List.of(role, "SUPERVISOR");
    }
    if ("UNIVERSITY_SUPERVISOR".equals(role)) {
      return List.of(role, "FACULTY");
    }
    return List.of(role);
  }

  private static List<String> readRoles(Map<?, ?> access) {
    Object roles = access.get("roles");
    if (roles instanceof List<?> list) {
      return list.stream().map(String::valueOf).toList();
    }
    return List.of();
  }
}
