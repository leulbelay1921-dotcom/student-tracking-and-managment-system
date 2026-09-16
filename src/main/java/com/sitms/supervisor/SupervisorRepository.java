package com.sitms.supervisor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link Supervisor}. */
public interface SupervisorRepository
    extends JpaRepository<Supervisor, UUID>, JpaSpecificationExecutor<Supervisor> {

  Optional<Supervisor> findByAppUserId(UUID appUserId);

  Optional<Supervisor> findByEmployeeNo(String employeeNo);

  boolean existsByEmployeeNo(String employeeNo);

  Page<Supervisor> findByOrganizationId(UUID organizationId, Pageable pageable);

  Page<Supervisor> findByActive(boolean active, Pageable pageable);

  @Query(
      "select distinct s.department from Supervisor s "
          + "where s.department is not null and trim(s.department) <> ''")
  List<String> findDistinctDepartments();
}
