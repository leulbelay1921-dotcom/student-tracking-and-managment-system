package com.sitms.academicstaff;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link AcademicStaff}. */
public interface AcademicStaffRepository
    extends JpaRepository<AcademicStaff, UUID>, JpaSpecificationExecutor<AcademicStaff> {

  Optional<AcademicStaff> findByAppUserId(UUID appUserId);

  Optional<AcademicStaff> findByStaffNo(String staffNo);

  boolean existsByStaffNo(String staffNo);

  Page<AcademicStaff> findByActive(boolean active, Pageable pageable);

  @Query(
      "select distinct a.department from AcademicStaff a "
          + "where a.department is not null and trim(a.department) <> ''")
  List<String> findDistinctDepartments();
}