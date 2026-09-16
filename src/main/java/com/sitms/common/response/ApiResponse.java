package com.sitms.common.response;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Standard API envelope returned by every endpoint.
 *
 * <p>Shape on success:</p>
 * <pre>{ "success": true, "message": "…", "data": {…} }</pre>
 *
 * @param success whether the request succeeded
 * @param message human-readable summary
 * @param data the payload (may be null)
 * @param errorCode a stable machine-readable code, null on success
 */
public record ApiResponse<T>(boolean success, String message, T data, String errorCode) {

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(true, "Success", data, null);
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return new ApiResponse<>(true, message, data, null);
  }

  public static <T> ApiResponse<T> error(String message, String errorCode) {
    return new ApiResponse<>(false, message, null, errorCode);
  }

  /** Wraps a Spring {@link Page} in a stable page shape. */
  public record PageData<T>(List<T> items, int page, int size, long totalElements, int totalPages) {
    public static <T> PageData<T> from(Page<T> page) {
      return new PageData<>(
          page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
          page.getTotalPages());
    }
  }
}
