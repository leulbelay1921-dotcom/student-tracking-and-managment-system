package com.sitms.organization;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link Organization}. */
public interface OrganizationRepository
    extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

  Optional<Organization> findByName(String name);

  boolean existsByName(String name);

  long countByActive(boolean active);

  @Query("select coalesce(sum(o.positions), 0) from Organization o")
  long sumPositions();

  Page<Organization> findByIndustry(String industry, Pageable pageable);

  Page<Organization> findByActive(boolean active, Pageable pageable);

  @Query(
      "select distinct o.industry from Organization o "
          + "where o.industry is not null and trim(o.industry) <> ''")
  List<String> findDistinctIndustries();
}
