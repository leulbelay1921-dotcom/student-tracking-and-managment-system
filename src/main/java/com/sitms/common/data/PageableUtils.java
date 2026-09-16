package com.sitms.common.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

/** Small paging helpers for list-based data that has not been paged by the database. */
public final class PageableUtils {

  /** Pages an in-memory list, applying an optional filter first. */
  public static <T> Page<T> fromList(List<T> source, Pageable pageable, Predicate<T> filter) {
    List<T> filtered = source.stream().filter(filter).collect(Collectors.toList());
    int start = (int) pageable.getOffset();
    List<T> content =
        start >= filtered.size()
            ? List.of()
            : new ArrayList<>(
                filtered.subList(start, Math.min(start + pageable.getPageSize(), filtered.size())));
    return new PageImpl<>(content, pageable, filtered.size());
  }

  private PageableUtils() {}
}