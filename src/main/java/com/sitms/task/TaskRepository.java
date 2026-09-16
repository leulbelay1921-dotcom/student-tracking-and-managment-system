package com.sitms.task;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Task}. */
public interface TaskRepository
    extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

  Page<Task> findByStudentId(UUID studentId, Pageable pageable);

  Page<Task> findByInternshipId(UUID internshipId, Pageable pageable);

  Page<Task> findBySupervisorId(UUID supervisorId, Pageable pageable);

  Page<Task> findByStatus(TaskStatus status, Pageable pageable);

  long countByStudentId(UUID studentId);

  long countByStudentIdAndStatus(UUID studentId, TaskStatus status);

  long countByInternshipId(UUID internshipId);

  long countByInternshipIdAndStatus(UUID internshipId, TaskStatus status);
}