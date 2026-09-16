package com.sitms.common.exception;

/**
 * Raised when a requested resource does not exist. Mapped to HTTP 404 with
 * {@link GlobalExceptionHandler#handleNotFound(ResourceNotFoundException)}.
 */
public class ResourceNotFoundException extends RuntimeException {

  private final String errorCode;

  public ResourceNotFoundException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
