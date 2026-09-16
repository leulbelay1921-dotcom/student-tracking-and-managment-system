package com.sitms.common.exception;

/**
 * Expected business/domain error, mapped by {@link GlobalExceptionHandler} to an HTTP 400-style
 * response with a stable {@code errorCode}. Use for validation rules and domain invariants, not for
 * programming errors.
 */
public class BusinessException extends RuntimeException {

  private final String errorCode;

  public BusinessException(String errorCode, String message) {
    super(message);
    this.errorCode = errorCode;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
