package com.sitms.internship;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link Internship}. */
public interface InternshipRepository
    extends JpaRepository<Internship, UUID>, JpaSpecificationExecutor<Internship> {

  long countBySupervisorIdAndStatus(UUID supervisorId, InternshipStatus status);

  long countByStatus(InternshipStatus status);

  long countByStatusIn(List<InternshipStatus> statuses);

  Page<Internship> findByStudentId(UUID studentId, Pageable pageable);

  List<Internship> findAllByStudentId(UUID studentId);

  Optional<Internship> findFirstByStudentIdOrderByCreatedAtDesc(UUID studentId);

  Page<Internship> findByStatus(InternshipStatus status, Pageable pageable);

  Page<Internship> findBySupervisorId(UUID supervisorId, Pageable pageable);

  Page<Internship> findByStudentIdIn(Collection<UUID> studentIds, Pageable pageable);

  Page<Internship> findByAcademicStaffId(UUID academicStaffId, Pageable pageable);

  boolean existsBySupervisorIdAndStudentId(UUID supervisorId, UUID studentId);

  boolean existsByAcademicStaffIdAndStudentId(UUID academicStaffId, UUID studentId);

  boolean existsByIdAndStudentId(UUID id, UUID studentId);

  boolean existsByStudentIdAndStatusIn(UUID studentId, List<InternshipStatus> statuses);

  /** Distinct student ids assigned to a supervisor. */
  @Query(
      "select distinct i.studentId from Internship i where i.supervisorId = :supervisorId")
  Set<UUID> findStudentIdsBySupervisorId(@org.springframework.data.repository.query.Param("supervisorId") UUID supervisorId);

  /** Distinct student ids assigned to an academic staff member. */
  @Query(
      "select distinct i.studentId from Internship i where i.academicStaffId = :academicStaffId")
  Set<UUID> findStudentIdsByAcademicStaffId(@org.springframework.data.repository.query.Param("academicStaffId") UUID academicStaffId);

  /** Count of internships grouped by status, as [status, count] rows ordered by status. */
  @Query("select i.status, count(i) from Internship i group by i.status order by i.status")
  List<Object[]> countByStatus();
}
