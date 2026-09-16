package com.sitms.attendance;

import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link LeaveRequest}. */
public interface LeaveRequestRepository
    extends JpaRepository<LeaveRequest, UUID>, JpaSpecificationExecutor<LeaveRequest> {

  Page<LeaveRequest> findByStudentId(UUID studentId, Pageable pageable);

  Page<LeaveRequest> findByStudentIdInOrderByCreatedAtDesc(
      Collection<UUID> studentIds, Pageable pageable);

  /** Overlapping-range guard: any request touching [startDate, endDate] for a student. */
  @Query(
      "select count(l) > 0 from LeaveRequest l "
          + "where l.studentId = :studentId and l.status = 'PENDING' "
          + " and l.startDate <= :endDate and l.endDate >= :startDate")
  boolean overlapsPending(
      @Param("studentId") UUID studentId,
      @Param("startDate") LocalDate startDate,
      @Param("endDate") LocalDate endDate);
}