package com.sitms.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.sitms.common.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void businessException_mapsToBadRequestWithCode() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleBusiness(new BusinessException("STUDENT_INACTIVE", "Student is inactive"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().success()).isFalse();
    assertThat(response.getBody().errorCode()).isEqualTo("STUDENT_INACTIVE");
  }

  @Test
  void notFound_mapsTo404WithCode() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleNotFound(new ResourceNotFoundException("STUDENT_NOT_FOUND", "No such student"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("STUDENT_NOT_FOUND");
  }

  @Test
  void unexpected_mapsTo500() {
    ResponseEntity<ApiResponse<Void>> response =
        handler.handleUnexpected(new IllegalStateException("boom"));
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().errorCode()).isEqualTo("INTERNAL_ERROR");
  }
}
