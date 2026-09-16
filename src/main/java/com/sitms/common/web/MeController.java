package com.sitms.common.web;

import com.sitms.common.response.ApiResponse;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the currently authenticated user's identity and roles.
 *
 * <p>Used by the frontend to render role-aware navigation (SITMS-005). Protected: requires a valid
 * JWT, so it doubles as a quick auth probe.
 */
@RestController
public class MeController {

  @GetMapping("/me")
  public ApiResponse<Me> me(JwtAuthenticationToken token) {
    List<String> roles =
        token.getAuthorities().stream().map(GrantedAuthority::getAuthority).sorted().toList();
    return ApiResponse.ok(
        "Authenticated.",
        new Me(token.getToken().getSubject(), token.getToken().getClaimAsString("preferred_username"), roles));
  }

  public record Me(String sub, String username, List<String> roles) {}
}
