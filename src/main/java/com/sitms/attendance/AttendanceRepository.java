package com.sitms.attendance;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Data access for {@link Attendance}. */
public interface AttendanceRepository
    extends JpaRepository<Attendance, UUID>, JpaSpecificationExecutor<Attendance> {

  Optional<Attendance> findByStudentIdAndAttendanceDate(UUID studentId, LocalDate date);

  long countByStudentIdAndStatus(UUID studentId, AttendanceStatus status);

  long countByInternshipIdAndStatus(UUID internshipId, AttendanceStatus status);
}