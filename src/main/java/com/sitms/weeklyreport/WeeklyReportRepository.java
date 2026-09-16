package com.sitms.weeklyreport;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/** Data access for {@link WeeklyReport}. */
@Repository
public interface WeeklyReportRepository
    extends JpaRepository<WeeklyReport, UUID>, JpaSpecificationExecutor<WeeklyReport> {

  Page<WeeklyReport> findAllByOrderByCreatedAtDesc(Pageable pageable);

  Page<WeeklyReport> findAllByStudentIdOrderByCreatedAtDesc(UUID studentId, Pageable pageable);

  Page<WeeklyReport> findByStudentIdInOrderByCreatedAtDesc(
      Collection<UUID> studentIds, Pageable pageable);
}