package com.sitms.academicevaluation;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link AcademicEvaluation}. */
public interface AcademicEvaluationRepository extends JpaRepository<AcademicEvaluation, UUID> {

  Optional<AcademicEvaluation> findByInternshipId(UUID internshipId);

  Page<AcademicEvaluation> findByStudentId(UUID studentId, Pageable pageable);

  Page<AcademicEvaluation> findByEvaluatorId(UUID evaluatorId, Pageable pageable);

  boolean existsByInternshipId(UUID internshipId);
}