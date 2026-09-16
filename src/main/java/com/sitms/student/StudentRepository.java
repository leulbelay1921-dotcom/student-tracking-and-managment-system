package com.sitms.student;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

/** Data access for {@link Student}. */
public interface StudentRepository
    extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {

  Optional<Student> findByAppUserId(UUID appUserId);

  Optional<Student> findByStudentNo(String studentNo);

  boolean existsByStudentNo(String studentNo);

  Page<Student> findByProgram(String program, Pageable pageable);

  /** Count of students grouped by program, as [program, count] rows ordered by program. */
  @Query("select s.program, count(s) from Student s group by s.program order by s.program")
  List<Object[]> countByProgram();
}