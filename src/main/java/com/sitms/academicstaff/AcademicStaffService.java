package com.sitms.academicstaff;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.response.Pagination;
import com.sitms.student.AppUserIdentity;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link AcademicStaff} (SITMS-072). */
@Service
public class AcademicStaffService {

  public static final String ACADEMIC_STAFF_NOT_FOUND = "ACADEMIC_STAFF_NOT_FOUND";
  public static final String DUPLICATE_ACADEMIC_STAFF = "DUPLICATE_ACADEMIC_STAFF";
  public static final String DUPLICATE_STAFF_NO = "DUPLICATE_STAFF_NO";
  public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";

  private final AcademicStaffRepository academicStaffRepository;
  private final AppUserIdentity appUserIdentity;

  public AcademicStaffService(
      AcademicStaffRepository academicStaffRepository, AppUserIdentity appUserIdentity) {
    this.academicStaffRepository = academicStaffRepository;
    this.appUserIdentity = appUserIdentity;
  }

  public Page<AcademicStaffResponse> list(
      Boolean active, String department, String q, Pageable pageable) {
    return academicStaffRepository
        .findAll(filters(active, department, q), pageable)
        .map(AcademicStaffResponse::from);
  }

  /** Distinct, non-blank departments across all academic staff, sorted. */
  public Page<String> departments(Pageable pageable) {
    List<String> all =
        academicStaffRepository.findDistinctDepartments().stream()
            .filter(d -> d != null && !d.isBlank())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    return Pagination.slice(all, pageable);
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<AcademicStaff> filters(
      Boolean active, String department, String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (active != null) {
        predicates.add(cb.equal(root.get("active"), active));
      }
      if (department != null && !department.isBlank()) {
        predicates.add(cb.equal(cb.lower(root.get("department")), department.trim().toLowerCase()));
      }
      if (q != null && !q.isBlank()) {
        String like = "%" + q.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("fullName")), like),
                cb.like(cb.lower(root.get("staffNo")), like),
                cb.like(cb.lower(root.get("department")), like)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public AcademicStaffResponse get(UUID id) {
    return AcademicStaffResponse.from(find(id));
  }

  /** Resolves the academic staff profile for the given app user id (used by /me). */
  @Transactional(readOnly = true)
  public AcademicStaffResponse byAppUserId(UUID appUserId) {
    return AcademicStaffResponse.from(
        academicStaffRepository
            .findByAppUserId(appUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ACADEMIC_STAFF_NOT_FOUND,
                        "No academic staff profile for user: " + appUserId)));
  }

  @Transactional
  public AcademicStaffResponse create(AcademicStaffRequest request) {
    if (academicStaffRepository.existsByStaffNo(request.staffNo())) {
      throw new BusinessException(DUPLICATE_STAFF_NO, "Staff number already exists.");
    }
    String username = request.appUserUsername();
    UUID appUserId =
        appUserIdentity
            .findIdByUsername(username)
            .orElseGet(() -> appUserIdentity.ensureRole(username, request.fullName(), "FACULTY"));
    if (academicStaffRepository.findByAppUserId(appUserId).isPresent()) {
      throw new BusinessException(
          DUPLICATE_ACADEMIC_STAFF, "An academic staff profile already exists for this user.");
    }
    AcademicStaff staff =
        academicStaffRepository.save(
            AcademicStaff.builder()
                .appUserId(appUserId)
                .appUserUsername(username)
                .staffNo(request.staffNo())
                .fullName(request.fullName())
                .title(request.title())
                .department(request.department())
                .active(request.activeOrDefault())
                .build());
    return AcademicStaffResponse.from(staff);
  }

  @Transactional
  public AcademicStaffResponse update(UUID id, AcademicStaffRequest request) {
    AcademicStaff staff = find(id);

    if (!staff.getStaffNo().equals(request.staffNo())
        && academicStaffRepository.existsByStaffNo(request.staffNo())) {
      throw new BusinessException(DUPLICATE_STAFF_NO, "Staff number already exists.");
    }

    String username = request.appUserUsername();
    if (username != null && !username.equals(staff.getAppUserUsername())) {
      if (appUserIdentity.existsByUsername(username)) {
        throw new BusinessException(DUPLICATE_USERNAME, "App user id already in use: " + username);
      }
      appUserIdentity.rename(staff.getAppUserId(), username);
      staff.renameTo(username);
    }

    staff.update(
        request.staffNo(),
        request.fullName(),
        request.title(),
        request.department(),
        request.activeOrDefault());
    return AcademicStaffResponse.from(academicStaffRepository.save(staff));
  }

  @Transactional
  public void delete(UUID id) {
    academicStaffRepository.delete(find(id));
  }

  private AcademicStaff find(UUID id) {
    return academicStaffRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ACADEMIC_STAFF_NOT_FOUND, "Academic staff not found: " + id));
  }
}