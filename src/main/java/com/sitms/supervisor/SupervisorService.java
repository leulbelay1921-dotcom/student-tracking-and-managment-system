package com.sitms.supervisor;

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

/** Business logic for {@link Supervisor} (SITMS-015). */
@Service
public class SupervisorService {

  public static final String SUPERVISOR_NOT_FOUND = "SUPERVISOR_NOT_FOUND";
  public static final String DUPLICATE_SUPERVISOR = "DUPLICATE_SUPERVISOR";
  public static final String DUPLICATE_EMPLOYEE_NO = "DUPLICATE_EMPLOYEE_NO";
  public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";

  private final SupervisorRepository supervisorRepository;
  private final AppUserIdentity appUserIdentity;

  public SupervisorService(
      SupervisorRepository supervisorRepository, AppUserIdentity appUserIdentity) {
    this.supervisorRepository = supervisorRepository;
    this.appUserIdentity = appUserIdentity;
  }

  public Page<SupervisorResponse> list(
      UUID organizationId, Boolean active, String department, String q, Pageable pageable) {
    return supervisorRepository
        .findAll(filters(organizationId, active, department, q), pageable)
        .map(SupervisorResponse::from);
  }

  /** Distinct, non-blank departments across all supervisors, sorted. */
  public Page<String> departments(Pageable pageable) {
    List<String> all =
        supervisorRepository.findDistinctDepartments().stream()
            .filter(d -> d != null && !d.isBlank())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    return Pagination.slice(all, pageable);
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Supervisor> filters(
      UUID organizationId, Boolean active, String department, String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (organizationId != null) {
        predicates.add(cb.equal(root.get("organizationId"), organizationId));
      }
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
                cb.like(cb.lower(root.get("employeeNo")), like),
                cb.like(cb.lower(root.get("department")), like)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public SupervisorResponse get(UUID id) {
    return SupervisorResponse.from(find(id));
  }

  /** Resolves the current supervisor's own profile from the JWT {@code sub}. */
  @Transactional(readOnly = true)
  public SupervisorResponse me(UUID appUserId) {
    return SupervisorResponse.from(
        supervisorRepository
            .findByAppUserId(appUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        SUPERVISOR_NOT_FOUND,
                        "No supervisor profile for user: " + appUserId)));
  }

  @Transactional
  public SupervisorResponse create(SupervisorRequest request) {
    if (supervisorRepository.existsByEmployeeNo(request.employeeNo())) {
      throw new BusinessException(DUPLICATE_EMPLOYEE_NO, "Employee number already exists.");
    }
    String username = request.appUserUsername();
    UUID appUserId =
        appUserIdentity
            .findIdByUsername(username)
            .orElseGet(
              () ->
                appUserIdentity.ensureRole(
                  username, request.fullName(), "SUPERVISOR", request.email()));
    if (supervisorRepository.findByAppUserId(appUserId).isPresent()) {
      throw new BusinessException(
          DUPLICATE_SUPERVISOR, "A supervisor profile already exists for this user.");
    }
    Supervisor supervisor =
        supervisorRepository.save(
            Supervisor.builder()
                .appUserId(appUserId)
                .appUserUsername(username)
                .organizationId(request.organizationId())
                .employeeNo(request.employeeNo())
                .fullName(request.fullName())
                .jobTitle(request.jobTitle())
                .department(request.department())
                .email(request.email())
                .phone(request.phone())
                .active(request.activeOrDefault())
                .build());
    return SupervisorResponse.from(supervisor);
  }

  @Transactional
  public SupervisorResponse update(UUID id, SupervisorRequest request) {
    Supervisor supervisor = find(id);

    if (!supervisor.getEmployeeNo().equals(request.employeeNo())
        && supervisorRepository.existsByEmployeeNo(request.employeeNo())) {
      throw new BusinessException(DUPLICATE_EMPLOYEE_NO, "Employee number already exists.");
    }

    String username = request.appUserUsername();
    if (username != null && !username.equals(supervisor.getAppUserUsername())) {
      if (appUserIdentity.existsByUsername(username)) {
        throw new BusinessException(DUPLICATE_USERNAME, "App user id already in use: " + username);
      }
      appUserIdentity.rename(supervisor.getAppUserId(), username);
      supervisor.renameTo(username);
    }

    supervisor.update(
        request.organizationId(),
        request.employeeNo(),
        request.fullName(),
        request.jobTitle(),
        request.department(),
        request.email(),
        request.phone(),
        request.activeOrDefault());
    return SupervisorResponse.from(supervisorRepository.save(supervisor));
  }

  @Transactional
  public void delete(UUID id) {
    supervisorRepository.delete(find(id));
  }

  private Supervisor find(UUID id) {
    return supervisorRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    SUPERVISOR_NOT_FOUND, "Supervisor not found: " + id));
  }
}
