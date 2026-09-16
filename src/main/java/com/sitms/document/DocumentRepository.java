package com.sitms.document;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for {@link Document}. */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  Page<Document> findByStudentId(UUID studentId, Pageable pageable);

  Page<Document> findByStudentIdAndStatus(UUID studentId, DocumentStatus status, Pageable pageable);

  Page<Document> findByInternshipId(UUID internshipId, Pageable pageable);

  Page<Document> findByStatus(DocumentStatus status, Pageable pageable);

  /** Documents of students assigned to a supervisor, scoped to a given status. */
  @Query(
      "select d from Document d "
          + "where d.status = :status and d.studentId in "
          + "(select i.studentId from Internship i where i.supervisorId = :supervisorId)")
  Page<Document> findByStatusAndAssignedSupervisor(
      @Param("status") DocumentStatus status,
      @Param("supervisorId") UUID supervisorId,
      Pageable pageable);

  /** Documents of students assigned to a supervisor. */
  @Query(
      "select d from Document d "
          + "where d.studentId in "
          + "(select i.studentId from Internship i where i.supervisorId = :supervisorId)")
  Page<Document> findByAssignedSupervisor(
      @Param("supervisorId") UUID supervisorId, Pageable pageable);

  /** Documents of students assigned to an academic staff member. */
  @Query(
      "select d from Document d "
          + "where d.studentId in "
          + "(select i.studentId from Internship i where i.academicStaffId = :academicStaffId)")
  Page<Document> findByAssignedAcademicStaff(
      @Param("academicStaffId") UUID academicStaffId, Pageable pageable);

  /** PENDING documents of students assigned to an academic staff member. */
  @Query(
      "select d from Document d "
          + "where d.status = :status and d.studentId in "
          + "(select i.studentId from Internship i where i.academicStaffId = :academicStaffId)")
  Page<Document> findByStatusAndAssignedAcademicStaff(
      @Param("status") DocumentStatus status,
      @Param("academicStaffId") UUID academicStaffId,
      Pageable pageable);
}