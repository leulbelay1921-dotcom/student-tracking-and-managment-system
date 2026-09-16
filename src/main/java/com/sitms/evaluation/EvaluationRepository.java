package com.sitms.evaluation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for {@link Evaluation} (SITMS-040). Student-facing reads are scoped by student,
 * supervisor lists by supervisor, and both the student and reports views filter by internship.
 */
public interface EvaluationRepository extends JpaRepository<Evaluation, UUID> {

  Page<Evaluation> findByStudentId(UUID studentId, Pageable pageable);

  Page<Evaluation> findByStudentIdAndStatus(
      UUID studentId, EvaluationStatus status, Pageable pageable);

  List<Evaluation> findByStudentIdAndInternshipId(UUID studentId, UUID internshipId);

  Page<Evaluation> findBySupervisorId(UUID supervisorId, Pageable pageable);

  List<Evaluation> findByInternshipId(UUID internshipId);

  boolean existsByStudentIdAndInternshipId(UUID studentId, UUID internshipId);

  boolean existsByStudentIdAndInternshipIdAndStatus(
      UUID studentId, UUID internshipId, EvaluationStatus status);

  /** Newest evaluation for a student, used for the progress-page academic standing (SITMS-033). */
  Optional<Evaluation> findFirstByStudentIdOrderByCreatedAtDesc(UUID studentId);
}