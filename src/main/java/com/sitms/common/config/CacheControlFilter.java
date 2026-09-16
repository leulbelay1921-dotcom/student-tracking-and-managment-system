package com.sitms.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Adds Cache-Control headers to read-heavy GET endpoints to reduce redundant network traffic.
 * Mutating endpoints and non-200 responses are never cached.
 */
@Component
public class CacheControlFilter extends OncePerRequestFilter {

  private static final Set<String> CACHEABLE_PATHS = Set.of(
      "/students",
      "/supervisors",
      "/organizations",
      "/internships",
      "/activities",
      "/tasks",
      "/documents",
      "/evaluations",
      "/attendance",
      "/analytics",
      "/reports"
  );

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    filterChain.doFilter(request, response);

    if ("GET".equalsIgnoreCase(request.getMethod())
        && response.getStatus() == HttpStatus.OK.value()
        && isCacheable(request.getRequestURI())) {
      String accept = request.getHeader("Accept");
      if (accept != null && accept.contains(MediaType.APPLICATION_JSON_VALUE)) {
        response.setHeader("Cache-Control", "max-age=60");
        response.setHeader("Vary", "Accept, Authorization");
      }
    }
  }

  private boolean isCacheable(String uri) {
    String path = uri.replaceFirst("^/api", "");
    return CACHEABLE_PATHS.stream().anyMatch(path::startsWith);
  }
}
