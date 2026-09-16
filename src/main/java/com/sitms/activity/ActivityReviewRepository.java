package com.sitms.activity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link ActivityReview}. */
public interface ActivityReviewRepository extends JpaRepository<ActivityReview, UUID> {

  Optional<ActivityReview> findByActivityId(UUID activityId);

  List<ActivityReview> findByActivityIdIn(Collection<UUID> activityIds);

  Page<ActivityReview> findByReviewerId(UUID reviewerId, Pageable pageable);

  long countByReviewerIdAndDecision(UUID reviewerId, ReviewDecision decision);
}
