package com.sitms.common.response;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/**
 * Helpers for producing a {@link Page} out of an already-built, sorted in-memory list.
 *
 * <p>Used by lookup and assignee endpoints whose rows need cross-entity enrichment that cannot be
 * expressed as a single pageable query.
 */
public final class Pagination {

  private Pagination() {}

  /** Returns the requested page of {@code all}; pages past the end yield an empty page. */
  public static <T> Page<T> slice(List<T> all, Pageable pageable) {
    long total = all.size();
    long start = pageable.getOffset();
    if (start >= total) {
      return new PageImpl<>(List.of(), pageable, total);
    }
    int end = (int) Math.min(start + pageable.getPageSize(), total);
    return new PageImpl<>(all.subList((int) start, end), pageable, total);
  }
}