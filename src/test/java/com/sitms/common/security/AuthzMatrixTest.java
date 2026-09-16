package com.sitms.common.security;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Authorization matrix (SITMS-054): verifies every endpoint's role gate lets exactly the intended
 * roles through and denies the rest (403).
 *
 * <p>Because a request with an invalid body is rejected (400) during argument binding before the
 * method-security proxy runs, write endpoints are exercised with a per-endpoint valid body so the
 * security decision is what is actually asserted. {@code allowed} roles must not be denied (any
 * non-403 outcome is acceptable); {@code denied} roles must be rejected with 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AuthzMatrixTest {

  private static final String ADMIN_SUB = "admin";
  private static final UUID STUDENT_APP_USER = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID SUPERVISOR_APP_USER = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID FACULTY_APP_USER = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final String STUDENT_SUB = STUDENT_APP_USER.toString();
  private static final String SUPERVISOR_SUB = SUPERVISOR_APP_USER.toString();
  private static final String FACULTY_SUB = FACULTY_APP_USER.toString();
  private static final String ID = "00000000-0000-0000-0000-000000000001";
  private static String uuid() {
    return UUID.randomUUID().toString();
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

  private RequestPostProcessor role(String sub, String role) {
    Jwt jwt =
        Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim(JwtClaimNames.SUB, sub)
            .claim("preferred_username", "user-" + sub)
            .claim("realm_access", Map.of("roles", List.of(role)))
            .build();
    return authentication(jwtAuthenticationConverter.convert(jwt));
  }

  private static Arguments c(String method, String path, String role, String sub, boolean allowed) {
    return Arguments.of(method, path, role, sub, allowed);
  }

  static Stream<Arguments> matrix() {
    return Stream.of(
        // ---- unauthenticated -> 401 ----
        c("GET", "/students", "NONE", null, false),
        c("GET", "/me", "NONE", null, false),
        c("GET", "/audit-logs", "NONE", null, false),

        // ---- admin-only: audit log read ----
        c("GET", "/audit-logs", "ADMIN", ADMIN_SUB, true),
        c("GET", "/audit-logs", "STUDENT", STUDENT_SUB, false),
        c("GET", "/audit-logs", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("GET", "/audit-logs", "FACULTY", FACULTY_SUB, false),

        // ---- open reads: any authenticated role ----
        c("GET", "/students", "ADMIN", ADMIN_SUB, true),
        c("GET", "/students", "STUDENT", STUDENT_SUB, true),
        c("GET", "/students", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/students", "FACULTY", FACULTY_SUB, true),
        c("GET", "/students/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/internships", "STUDENT", STUDENT_SUB, true),
        c("GET", "/internships", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/internships", "FACULTY", FACULTY_SUB, true),
        c("GET", "/internships/me", "STUDENT", STUDENT_SUB, true),
        c("GET", "/internships/me", "ADMIN", ADMIN_SUB, false),
        c("GET", "/internships/me", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("GET", "/internships/me", "FACULTY", FACULTY_SUB, false),
        c("GET", "/academic-staff", "ADMIN", ADMIN_SUB, true),
        c("GET", "/academic-staff", "STUDENT", STUDENT_SUB, true),
        c("GET", "/academic-staff", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/academic-staff", "FACULTY", FACULTY_SUB, true),
        c("GET", "/academic-staff/me", "FACULTY", FACULTY_SUB, true),
        c("GET", "/academic-staff/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/activities", "STUDENT", STUDENT_SUB, true),
        c("GET", "/activities", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/activities", "FACULTY", FACULTY_SUB, true),
        c("GET", "/tasks", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/tasks", "STUDENT", STUDENT_SUB, true),
        c("GET", "/supervisors", "STUDENT", STUDENT_SUB, true),
        c("GET", "/supervisors", "FACULTY", FACULTY_SUB, true),
        c("GET", "/organizations", "STUDENT", STUDENT_SUB, true),
        c("GET", "/progress", "STUDENT", STUDENT_SUB, true),
        c("GET", "/progress", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/progress", "FACULTY", FACULTY_SUB, true),
        c("GET", "/attendance", "STUDENT", STUDENT_SUB, true),
        c("GET", "/attendance", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/attendance", "FACULTY", FACULTY_SUB, true),
        c("GET", "/attendance", "ADMIN", ADMIN_SUB, true),
        c("GET", "/attendance/summary", "STUDENT", STUDENT_SUB, true),
        c("GET", "/attendance/summary", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/attendance/summary", "FACULTY", FACULTY_SUB, true),
        c("GET", "/attendance/summary", "ADMIN", ADMIN_SUB, true),
        c("GET", "/me", "ADMIN", ADMIN_SUB, true),
        c("GET", "/me", "STUDENT", STUDENT_SUB, true),
        c("GET", "/me", "SUPERVISOR", SUPERVISOR_SUB, true),

        // ---- applications: reads open, submit/withdraw student-only, decide admin|supervisor ----
        c("GET", "/applications", "ADMIN", ADMIN_SUB, true),
        c("GET", "/applications", "STUDENT", STUDENT_SUB, true),
        c("GET", "/applications", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/applications", "FACULTY", FACULTY_SUB, true),
        c("GET", "/applications/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/applications/" + ID, "ADMIN", ADMIN_SUB, true),
        c("GET", "/applications/" + ID, "FACULTY", FACULTY_SUB, true),
        c("POST", "/applications", "STUDENT", STUDENT_SUB, true),
        c("POST", "/applications", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/applications", "ADMIN", ADMIN_SUB, false),
        c("POST", "/applications", "FACULTY", FACULTY_SUB, false),
        c("POST", "/applications/" + ID + "/withdraw", "STUDENT", STUDENT_SUB, true),
        c("POST", "/applications/" + ID + "/withdraw", "ADMIN", ADMIN_SUB, false),
        c("POST", "/applications/" + ID + "/withdraw", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("PATCH", "/applications/" + ID + "/status", "ADMIN", ADMIN_SUB, true),
        c("PATCH", "/applications/" + ID + "/status", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/applications/" + ID + "/status", "STUDENT", STUDENT_SUB, false),
        c("PATCH", "/applications/" + ID + "/status", "FACULTY", FACULTY_SUB, false),

        // ---- students/me and student-only writes ----
        c("GET", "/students/me", "STUDENT", STUDENT_SUB, true),
        c("GET", "/students/me", "FACULTY", FACULTY_SUB, false),
        c("GET", "/students/me", "ADMIN", ADMIN_SUB, false),
        c("GET", "/students/me", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("PUT", "/students/me", "STUDENT", STUDENT_SUB, true),
        c("PUT", "/students/me", "ADMIN", ADMIN_SUB, false),
        c("POST", "/students/me/photo", "STUDENT", STUDENT_SUB, true),
        c("POST", "/students/me/photo", "ADMIN", ADMIN_SUB, false),
        c("POST", "/students/me/photo", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/students/" + ID + "/photo", "ADMIN", ADMIN_SUB, true),
        c("POST", "/students/" + ID + "/photo", "STUDENT", STUDENT_SUB, false),
        c("POST", "/students/" + ID + "/photo", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/activities/" + ID + "/submit", "STUDENT", STUDENT_SUB, true),
        c("POST", "/activities/" + ID + "/submit", "ADMIN", ADMIN_SUB, false),
        c("POST", "/activities/" + ID + "/submit", "SUPERVISOR", SUPERVISOR_SUB, false),

        // ---- attendance writes: supervisor-only record of present/absent ----
        c("POST", "/attendance/record", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/attendance/record", "STUDENT", STUDENT_SUB, false),
        c("POST", "/attendance/record", "ADMIN", ADMIN_SUB, false),

        // ---- attendance self-service: student-only check-in/out, leave, issue reports ----
        c("POST", "/attendance/check-in", "STUDENT", STUDENT_SUB, true),
        c("POST", "/attendance/check-in", "ADMIN", ADMIN_SUB, false),
        c("POST", "/attendance/check-in", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/attendance/check-out", "STUDENT", STUDENT_SUB, true),
        c("POST", "/attendance/check-out", "ADMIN", ADMIN_SUB, false),
        c("GET", "/attendance/leave-requests", "STUDENT", STUDENT_SUB, true),
        c("GET", "/attendance/leave-requests", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/attendance/leave-requests", "FACULTY", FACULTY_SUB, true),
        c("GET", "/attendance/leave-requests", "ADMIN", ADMIN_SUB, true),
        c("POST", "/attendance/leave-requests", "STUDENT", STUDENT_SUB, true),
        c("POST", "/attendance/leave-requests", "ADMIN", ADMIN_SUB, false),
        c("POST", "/attendance/leave-requests", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/attendance/leave-requests", "FACULTY", FACULTY_SUB, false),
        c("POST", "/attendance/leave-requests/" + ID + "/review", "ADMIN", ADMIN_SUB, true),
        c("POST", "/attendance/leave-requests/" + ID + "/review", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/attendance/leave-requests/" + ID + "/review", "STUDENT", STUDENT_SUB, false),
        c("POST", "/attendance/leave-requests/" + ID + "/review", "FACULTY", FACULTY_SUB, false),
        c("GET", "/attendance/issues", "STUDENT", STUDENT_SUB, true),
        c("GET", "/attendance/issues", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/attendance/issues", "FACULTY", FACULTY_SUB, true),
        c("GET", "/attendance/issues", "ADMIN", ADMIN_SUB, true),
        c("POST", "/attendance/issues", "STUDENT", STUDENT_SUB, true),
        c("POST", "/attendance/issues", "ADMIN", ADMIN_SUB, false),
        c("POST", "/attendance/issues", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/attendance/issues/" + ID + "/review", "ADMIN", ADMIN_SUB, true),
        c("POST", "/attendance/issues/" + ID + "/review", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/attendance/issues/" + ID + "/review", "STUDENT", STUDENT_SUB, false),
        c("POST", "/attendance/issues/" + ID + "/review", "FACULTY", FACULTY_SUB, false),

        // ---- weekly reports: all read; student edit; two-stage reviews by company then academic ----
        c("GET", "/weekly-reports", "STUDENT", STUDENT_SUB, true),
        c("GET", "/weekly-reports", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/weekly-reports", "FACULTY", FACULTY_SUB, true),
        c("GET", "/weekly-reports", "ADMIN", ADMIN_SUB, true),
        c("GET", "/weekly-reports/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/weekly-reports/" + ID, "FACULTY", FACULTY_SUB, true),
        c("POST", "/weekly-reports", "STUDENT", STUDENT_SUB, true),
        c("POST", "/weekly-reports", "ADMIN", ADMIN_SUB, false),
        c("POST", "/weekly-reports", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/weekly-reports", "FACULTY", FACULTY_SUB, false),
        c("PUT", "/weekly-reports/" + ID, "STUDENT", STUDENT_SUB, true),
        c("PUT", "/weekly-reports/" + ID, "ADMIN", ADMIN_SUB, false),
        c("POST", "/weekly-reports/" + ID + "/company-review", "ADMIN", ADMIN_SUB, true),
        c("POST", "/weekly-reports/" + ID + "/company-review", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/weekly-reports/" + ID + "/company-review", "STUDENT", STUDENT_SUB, false),
        c("POST", "/weekly-reports/" + ID + "/company-review", "FACULTY", FACULTY_SUB, false),
        c("POST", "/weekly-reports/" + ID + "/academic-review", "ADMIN", ADMIN_SUB, true),
        c("POST", "/weekly-reports/" + ID + "/academic-review", "FACULTY", FACULTY_SUB, true),
        c("POST", "/weekly-reports/" + ID + "/academic-review", "STUDENT", STUDENT_SUB, false),
        c("POST", "/weekly-reports/" + ID + "/academic-review", "SUPERVISOR", SUPERVISOR_SUB, false),

        // ---- admin-only writes: denied for student & supervisor ----
        c("POST", "/students", "STUDENT", STUDENT_SUB, false),
        c("POST", "/students", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/students", "ADMIN", ADMIN_SUB, true),
        c("PUT", "/students/" + ID, "STUDENT", STUDENT_SUB, false),
        c("PUT", "/students/" + ID, "ADMIN", ADMIN_SUB, true),
        c("DELETE", "/students/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("DELETE", "/students/" + ID, "ADMIN", ADMIN_SUB, true),
        c("POST", "/internships", "STUDENT", STUDENT_SUB, false),
        c("POST", "/internships", "ADMIN", ADMIN_SUB, true),
        c("PUT", "/internships/" + ID, "STUDENT", STUDENT_SUB, false),
        c("DELETE", "/internships/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/tasks", "STUDENT", STUDENT_SUB, false),
        c("POST", "/tasks", "ADMIN", ADMIN_SUB, false),
        c("POST", "/tasks", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PUT", "/tasks/" + ID, "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PUT", "/tasks/" + ID, "ADMIN", ADMIN_SUB, false),
        c("PUT", "/tasks/" + ID, "STUDENT", STUDENT_SUB, false),
        c("DELETE", "/tasks/" + ID, "SUPERVISOR", SUPERVISOR_SUB, true),
        c("DELETE", "/tasks/" + ID, "ADMIN", ADMIN_SUB, false),
        c("DELETE", "/tasks/" + ID, "STUDENT", STUDENT_SUB, false),
        c("POST", "/supervisors", "STUDENT", STUDENT_SUB, false),
        c("PUT", "/supervisors/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("DELETE", "/supervisors/" + ID, "STUDENT", STUDENT_SUB, false),
        c("POST", "/organizations", "STUDENT", STUDENT_SUB, false),
        c("PUT", "/organizations/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("DELETE", "/organizations/" + ID, "STUDENT", STUDENT_SUB, false),
        c("PUT", "/activities/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("PUT", "/activities/" + ID, "ADMIN", ADMIN_SUB, true),
        c("DELETE", "/activities/" + ID, "STUDENT", STUDENT_SUB, false),
        c("PUT", "/activities/" + ID, "STUDENT", STUDENT_SUB, true), // self-service revision edits

        // ---- activities: create = admin|student; review = admin|supervisor|faculty ----
        c("POST", "/activities", "STUDENT", STUDENT_SUB, true),
        c("POST", "/activities", "ADMIN", ADMIN_SUB, true),
        c("POST", "/activities", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/activities/" + ID + "/review", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/activities/" + ID + "/review", "ADMIN", ADMIN_SUB, true),
        c("POST", "/activities/" + ID + "/review", "STUDENT", STUDENT_SUB, false),
        c("POST", "/activities/" + ID + "/review", "FACULTY", FACULTY_SUB, true),

        // ---- reports: all four roles, scoped per role ----
        c("GET", "/reports/internships", "ADMIN", ADMIN_SUB, true),
        c("GET", "/reports/internships", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/reports/internships", "FACULTY", FACULTY_SUB, true),
        c("GET", "/reports/internships", "STUDENT", STUDENT_SUB, true),
        c("GET", "/reports/activities", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/reports/activities", "FACULTY", FACULTY_SUB, true),
        c("GET", "/reports/activities", "STUDENT", STUDENT_SUB, true),
        c("GET", "/reports/evaluations", "ADMIN", ADMIN_SUB, true),
        c("GET", "/reports/evaluations", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/reports/evaluations", "FACULTY", FACULTY_SUB, true),
        c("GET", "/reports/evaluations", "STUDENT", STUDENT_SUB, true),
        c("GET", "/reports/internships/export", "ADMIN", ADMIN_SUB, true),
        c("GET", "/reports/internships/export", "FACULTY", FACULTY_SUB, true),
        c("GET", "/reports/internships/export", "STUDENT", STUDENT_SUB, true),
        c("GET", "/analytics", "ADMIN", ADMIN_SUB, true),
        c("GET", "/analytics", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/analytics", "STUDENT", STUDENT_SUB, false),
        c("GET", "/activities/review-queue", "ADMIN", ADMIN_SUB, true),
        c("GET", "/activities/review-queue", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/activities/review-queue", "FACULTY", FACULTY_SUB, true),
        c("GET", "/activities/review-queue", "STUDENT", STUDENT_SUB, false),
        c("GET", "/documents/review-queue", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/documents/review-queue", "STUDENT", STUDENT_SUB, false),
        c("GET", "/documents/review-queue", "FACULTY", FACULTY_SUB, false),
        c("DELETE", "/documents/" + ID, "FACULTY", FACULTY_SUB, false),
        c("PATCH", "/internships/" + ID + "/status", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/internships/" + ID + "/status", "STUDENT", STUDENT_SUB, false),
        c("PATCH", "/tasks/" + ID + "/status", "STUDENT", STUDENT_SUB, true),
        c("PATCH", "/tasks/" + ID + "/status", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/tasks/" + ID + "/status", "ADMIN", ADMIN_SUB, false),

        // ---- evaluations: all read; admin+supervisor write; admin-only delete ----
        c("GET", "/evaluations", "STUDENT", STUDENT_SUB, true),
        c("GET", "/evaluations", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/evaluations", "ADMIN", ADMIN_SUB, true),
        c("GET", "/evaluations", "FACULTY", FACULTY_SUB, true),
        c("GET", "/evaluations/" + ID, "FACULTY", FACULTY_SUB, true),
        c("GET", "/evaluations/assignees", "ADMIN", ADMIN_SUB, true),
        c("GET", "/evaluations/assignees", "FACULTY", FACULTY_SUB, true),
        c("POST", "/evaluations", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/evaluations", "ADMIN", ADMIN_SUB, true),
        c("POST", "/evaluations", "STUDENT", STUDENT_SUB, false),
        c("POST", "/evaluations", "FACULTY", FACULTY_SUB, false),
        c("PUT", "/evaluations/" + ID, "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PUT", "/evaluations/" + ID, "STUDENT", STUDENT_SUB, false),
        c("PUT", "/evaluations/" + ID, "FACULTY", FACULTY_SUB, false),
        c("DELETE", "/evaluations/" + ID, "ADMIN", ADMIN_SUB, true),
        c("DELETE", "/evaluations/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("DELETE", "/evaluations/" + ID, "STUDENT", STUDENT_SUB, false),
        c("DELETE", "/evaluations/" + ID, "FACULTY", FACULTY_SUB, false),

        // ---- academic evaluations: admin|faculty|student read; admin|faculty write ----
        c("GET", "/academic-evaluations", "ADMIN", ADMIN_SUB, true),
        c("GET", "/academic-evaluations", "FACULTY", FACULTY_SUB, true),
        c("GET", "/academic-evaluations", "STUDENT", STUDENT_SUB, true),
        c("GET", "/academic-evaluations", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("GET", "/academic-evaluations/assignees", "ADMIN", ADMIN_SUB, true),
        c("GET", "/academic-evaluations/assignees", "FACULTY", FACULTY_SUB, true),
        c("GET", "/academic-evaluations/assignees", "STUDENT", STUDENT_SUB, false),
        c("GET", "/academic-evaluations/assignees", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("GET", "/academic-evaluations/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/academic-evaluations/" + ID, "FACULTY", FACULTY_SUB, true),
        c("GET", "/academic-evaluations/by-internship/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/academic-evaluations/by-internship/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),
        c("PUT", "/academic-evaluations", "ADMIN", ADMIN_SUB, true),
        c("PUT", "/academic-evaluations", "FACULTY", FACULTY_SUB, true),
        c("PUT", "/academic-evaluations", "STUDENT", STUDENT_SUB, false),
        c("PUT", "/academic-evaluations", "SUPERVISOR", SUPERVISOR_SUB, false),

        // ---- documents: all read; admin+student upload; admin+supervisor review ----
        c("GET", "/documents", "STUDENT", STUDENT_SUB, true),
        c("GET", "/documents", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/documents", "ADMIN", ADMIN_SUB, true),
        c("GET", "/documents", "FACULTY", FACULTY_SUB, true),
        c("GET", "/documents/" + ID + "/download", "FACULTY", FACULTY_SUB, true),
        c("POST", "/documents", "STUDENT", STUDENT_SUB, true),
        c("POST", "/documents", "ADMIN", ADMIN_SUB, true),
        c("POST", "/documents", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/documents", "FACULTY", FACULTY_SUB, false),
        c("POST", "/documents/" + ID + "/review", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/documents/" + ID + "/review", "ADMIN", ADMIN_SUB, true),
        c("POST", "/documents/" + ID + "/review", "STUDENT", STUDENT_SUB, false),

        // ---- notifications: all roles ----
        c("GET", "/notifications", "STUDENT", STUDENT_SUB, true),
        c("GET", "/notifications", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/notifications", "ADMIN", ADMIN_SUB, true),
        c("GET", "/notifications/unread-count", "STUDENT", STUDENT_SUB, true),
        c("GET", "/notifications/unread-count", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/notifications/" + ID + "/read", "STUDENT", STUDENT_SUB, true),
        c("PATCH", "/notifications/" + ID + "/read", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/notifications/read-all", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/notifications/read-all", "ADMIN", ADMIN_SUB, true),

        // ---- notification broadcast: all roles ----
        c("POST", "/notifications/broadcast", "ADMIN", ADMIN_SUB, true),
        c("POST", "/notifications/broadcast", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/notifications/broadcast", "FACULTY", FACULTY_SUB, true),
        c("POST", "/notifications/broadcast", "STUDENT", STUDENT_SUB, true),

        // ---- tickets: reads open; any role may raise; updates resolve/assign = admin|supervisor|faculty ----
        c("GET", "/tickets", "ADMIN", ADMIN_SUB, true),
        c("GET", "/tickets", "STUDENT", STUDENT_SUB, true),
        c("GET", "/tickets", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("GET", "/tickets", "FACULTY", FACULTY_SUB, true),
        c("GET", "/tickets/" + ID, "STUDENT", STUDENT_SUB, true),
        c("GET", "/tickets/" + ID, "FACULTY", FACULTY_SUB, true),
        c("POST", "/tickets", "STUDENT", STUDENT_SUB, true),
        c("POST", "/tickets", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("POST", "/tickets", "FACULTY", FACULTY_SUB, true),
        c("POST", "/tickets", "ADMIN", ADMIN_SUB, true),
        c("PATCH", "/tickets/" + ID, "ADMIN", ADMIN_SUB, true),
        c("PATCH", "/tickets/" + ID, "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/tickets/" + ID, "FACULTY", FACULTY_SUB, true),
        c("PATCH", "/tickets/" + ID, "STUDENT", STUDENT_SUB, false),

        // ---- academic staff CRUD: reads open; writes admin-only ----
        c("POST", "/academic-staff", "ADMIN", ADMIN_SUB, true),
        c("POST", "/academic-staff", "SUPERVISOR", SUPERVISOR_SUB, false),
        c("POST", "/academic-staff", "FACULTY", FACULTY_SUB, false),
        c("PUT", "/academic-staff/" + ID, "ADMIN", ADMIN_SUB, true),
        c("PUT", "/academic-staff/" + ID, "STUDENT", STUDENT_SUB, false),
        c("DELETE", "/academic-staff/" + ID, "ADMIN", ADMIN_SUB, true),
        c("DELETE", "/academic-staff/" + ID, "SUPERVISOR", SUPERVISOR_SUB, false),

        // ---- supervisor assignment: admin + the assigned supervisor may hand a student over ----
        c("PATCH", "/internships/" + ID + "/supervisors", "ADMIN", ADMIN_SUB, true),
        c("PATCH", "/internships/" + ID + "/supervisors", "SUPERVISOR", SUPERVISOR_SUB, true),
        c("PATCH", "/internships/" + ID + "/supervisors", "FACULTY", FACULTY_SUB, false),
        c("PATCH", "/internships/" + ID + "/supervisors", "STUDENT", STUDENT_SUB, false));
  }

  private static String bodyFor(String path) {
    String p = path.replace(ID, "00000000-0000-0000-0000-000000000099");
    if (p.startsWith("/notifications/broadcast")) {
      return "{\"audience\":\"ALL_STUDENTS\",\"title\":\"Notice\",\"body\":\"b\","
          + "\"link\":\"/internships\"}";
    }
    if (p.startsWith("/attendance")) {
      if (p.endsWith("/check-in")) {
        return "{\"internshipId\":\"" + uuid() + "\",\"date\":\"2026-01-01\"}";
      }
      if (p.endsWith("/check-out")) {
        return "{\"date\":\"2026-01-01\"}";
      }
      if (p.equals("/attendance/leave-requests")) {
        return "{\"internshipId\":\"" + uuid() + "\",\"startDate\":\"2026-01-01\","
            + "\"endDate\":\"2026-01-02\",\"reason\":\"r\"}";
      }
      if (p.equals("/attendance/issues")) {
        return "{\"attendanceId\":\"" + uuid() + "\",\"message\":\"m\"}";
      }
      if (p.endsWith("/review")) {
        return "{\"decision\":\"APPROVED\",\"feedback\":\"ok\"}";
      }
      return "{\"studentId\":\"" + uuid() + "\",\"status\":\"PRESENT\"}";
    }
    if (p.startsWith("/weekly-reports")) {
      if (p.contains("-review")) {
        return "{\"approved\":true,\"comment\":\"ok\"}";
      }
      return "{\"weekNumber\":1,\"content\":\"c\"}";
    }
    if (p.startsWith("/students")) {
      return "{\"appUserUsername\":\"fk-matrix\",\"studentNo\":\"S-001\",\"fullName\":\"A A\","
          + "\"program\":\"CS\",\"level\":1}";
    }
    if (p.startsWith("/academic-staff")) {
      return "{\"appUserUsername\":\"fk-matrix\",\"staffNo\":\"F-1\",\"fullName\":\"N N\","
          + "\"title\":\"Prof\",\"department\":\"D\",\"active\":true}";
    }
    if (p.startsWith("/tickets")) {
      if (p.equals("/tickets")) {
        return "{\"internshipId\":\"" + uuid() + "\",\"title\":\"Help\",\"description\":\"d\","
            + "\"priority\":\"MEDIUM\"}";
      }
      return "{\"status\":\"RESOLVED\"}";
    }
    if (p.startsWith("/internships")) {
      if (p.endsWith("/supervisors")) {
        return "{}";
      }
      if (p.endsWith("/status")) {
        return "{\"status\":\"APPROVED\"}";
      }
      if (p.contains("/register")) {
        return "{\"organizationId\":\"" + uuid() + "\",\"title\":\"T\",\"description\":\"d\","
            + "\"startDate\":\"2026-01-01\",\"endDate\":\"2026-06-01\"}";
      }
      return "{\"studentId\":\"" + uuid() + "\",\"organizationId\":\"" + uuid()
          + "\",\"title\":\"T\",\"description\":\"d\",\"startDate\":\"2026-01-01\","
          + "\"endDate\":\"2026-06-01\"}";
    }
    if (p.startsWith("/tasks")) {
      if (p.endsWith("/status")) {
        return "{\"status\":\"IN_PROGRESS\"}";
      }
      return "{\"studentId\":\"" + uuid() + "\",\"title\":\"T\",\"description\":\"d\","
          + "\"startDate\":\"2026-01-01\",\"dueDate\":\"2026-01-02\",\"category\":\"Development\","
          + "\"expectedOutcome\":\"Working feature\",\"priority\":\"HIGH\"}";
    }
    if (p.startsWith("/supervisors")) {
      return "{\"appUserUsername\":\"sup-matrix\",\"organizationId\":\"" + uuid()
          + "\",\"employeeNo\":\"E-1\",\"fullName\":\"N N\",\"jobTitle\":\"J\","
          + "\"department\":\"D\",\"active\":true}";
    }
    if (p.startsWith("/organizations")) {
      return "{\"name\":\"Org One\",\"industry\":\"IT\"}";
    }
    if (p.startsWith("/applications")) {
      if (p.endsWith("/status")) {
        return "{\"status\":\"APPROVED\"}";
      }
      if (p.endsWith("/withdraw")) {
        return "{}";
      }
      return "{\"organizationId\":\"" + uuid() + "\",\"title\":\"My Application\"}";
    }
    if (p.startsWith("/activities")) {
      if (p.endsWith("/review")) {
        return "{\"decision\":\"APPROVED\",\"feedback\":\"ok\"}";
      }
      if (p.endsWith("/submit")) {
        return "{}";
      }
      return "{\"studentId\":\"" + uuid() + "\",\"activityDate\":\"2026-01-01\","
          + "\"type\":\"DAILY\",\"description\":\"d\",\"hours\":1}";
    }
    if (p.startsWith("/evaluations")) {
      return "{\"studentId\":\"" + uuid()
          + "\",\"technicalSkills\":1,\"communication\":1,\"teamwork\":1,"
          + "\"problemSolving\":1,\"professionalism\":1}";
    }
    if (p.startsWith("/academic-evaluations")) {
      return "{\"internshipId\":\"" + uuid() + "\",\"studentId\":\"" + uuid()
          + "\",\"academicLearning\":1,\"problemSolving\":1,\"documentation\":1,"
          + "\"technicalDevelopment\":1,\"professionalism\":1,\"finalPresentation\":1}";
    }
    if (p.startsWith("/documents")) {
      if (p.endsWith("/review")) {
        return "{\"decision\":\"APPROVED\",\"feedback\":\"ok\"}";
      }
      return "{}";
    }
    return "{}";
  }

  @ParameterizedTest(name = "{0} {1} as {2} denied={4}")
  @MethodSource("matrix")
  void endpointEnforcesRoleGate(String method, String path, String role, String sub, boolean allowed)
      throws Exception {
    if (role.equals("NONE")) {
      mockMvc.perform(build(method, path, null)).andExpect(status().isUnauthorized());
      return;
    }
    RequestPostProcessor token = role(sub, role);
    int status =
        mockMvc
            .perform(build(method, path, token))
            .andReturn()
            .getResponse()
            .getStatus();
    boolean denied = status == 403;
    assertTrue(
        denied ? !allowed : allowed,
        (denied ? "was denied (403) but expected allowed" : "was allowed (status=" + status
            + ") but expected denied")
            + " for " + method + " " + path + " as " + role);
  }

  private MockHttpServletRequestBuilder build(String method, String path, RequestPostProcessor token) {
    MockHttpServletRequestBuilder request;
    if (path.equals("/documents") && method.equals("POST")
        || (path.endsWith("/photo") && method.equals("POST"))) {
      request =
          multipart(path)
              .file(new MockMultipartFile("file", "photo.png", "image/png", new byte[]{1}));
    } else {
      String body = bodyFor(path);
      request =
          switch (method) {
            case "GET" -> get(path);
            case "POST" -> post(path).content(body).contentType(MediaType.APPLICATION_JSON);
            case "PUT" -> put(path).content(body).contentType(MediaType.APPLICATION_JSON);
            case "PATCH" -> patch(path).content(body).contentType(MediaType.APPLICATION_JSON);
            case "DELETE" -> delete(path);
            default -> throw new IllegalArgumentException(method);
          };
    }
    if (token != null) {
      request.with(token);
    }
    return request;
  }
}
