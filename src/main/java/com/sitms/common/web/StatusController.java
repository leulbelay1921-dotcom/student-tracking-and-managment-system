package com.sitms.common.web;

import com.sitms.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal status endpoints to verify the API envelope and app boot. */
@RestController
public class StatusController {

  @GetMapping("/")
  public ApiResponse<Status> status() {
    return ApiResponse.ok("SITMS backend is running.", new Status("ok"));
  }

  public record Status(String status) {}
}
