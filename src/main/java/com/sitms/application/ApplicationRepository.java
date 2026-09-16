package com.sitms.application;

import java.util.Collection;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Application}. */
public interface ApplicationRepository
    extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {

  /** Whether the student already has an active (pending or approved) application on file. */
  boolean existsByStudentIdAndStatusIn(UUID studentId, Collection<ApplicationStatus> statuses);
}