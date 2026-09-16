package com.sitms.attendance;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link AttendanceIssue}. */
public interface AttendanceIssueRepository
    extends JpaRepository<AttendanceIssue, UUID>, JpaSpecificationExecutor<AttendanceIssue> {

  Page<AttendanceIssue> findByStudentId(UUID studentId, Pageable pageable);

  Page<AttendanceIssue> findByStudentIdInOrderByCreatedAtDesc(
      Collection<UUID> studentIds, Pageable pageable);
}