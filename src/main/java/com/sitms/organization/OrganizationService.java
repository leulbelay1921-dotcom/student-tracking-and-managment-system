package com.sitms.organization;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.response.Pagination;
import com.sitms.internship.InternshipRepository;
import com.sitms.internship.InternshipStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for {@link Organization} (SITMS-012). */
@Service
public class OrganizationService {

  public static final String ORGANIZATION_NOT_FOUND = "ORGANIZATION_NOT_FOUND";
  public static final String DUPLICATE_ORGANIZATION = "DUPLICATE_ORGANIZATION";

  private final OrganizationRepository organizationRepository;
  private final InternshipRepository internshipRepository;

  public OrganizationService(
      OrganizationRepository organizationRepository,
      InternshipRepository internshipRepository) {
    this.organizationRepository = organizationRepository;
    this.internshipRepository = internshipRepository;
  }

  public Page<OrganizationResponse> list(
      Boolean active, String industry, String q, Pageable pageable) {
    return organizationRepository
        .findAll(filters(active, industry, q), pageable)
        .map(OrganizationResponse::from);
  }

  /** Distinct, non-blank industries across all organizations, sorted. */
  public Page<String> industries(Pageable pageable) {
    List<String> all =
        organizationRepository.findDistinctIndustries().stream()
            .filter(i -> i != null && !i.isBlank())
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
    return Pagination.slice(all, pageable);
  }

  /** Aggregate KPIs: partner counts, total open positions, placements and active interns. */
  @Transactional(readOnly = true)
  public OrganizationStats stats() {
    long placed =
        internshipRepository.countByStatusIn(
            List.of(InternshipStatus.APPROVED, InternshipStatus.COMPLETED));
    return new OrganizationStats(
        organizationRepository.count(),
        organizationRepository.countByActive(true),
        organizationRepository.sumPositions(),
        placed,
        internshipRepository.countByStatus(InternshipStatus.APPROVED));
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Organization> filters(Boolean active, String industry, String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (active != null) {
        predicates.add(cb.equal(root.get("active"), active));
      }
      if (industry != null && !industry.isBlank()) {
        predicates.add(cb.equal(cb.lower(root.get("industry")), industry.trim().toLowerCase()));
      }
      if (q != null && !q.isBlank()) {
        String like = "%" + q.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("industry")), like),
                cb.like(cb.lower(root.get("contactEmail")), like)));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public OrganizationResponse get(UUID id) {
    return OrganizationResponse.from(find(id));
  }

  @Transactional
  public OrganizationResponse create(OrganizationRequest request) {
    if (organizationRepository.existsByName(request.name())) {
      throw new BusinessException(DUPLICATE_ORGANIZATION, "Organization name already exists.");
    }
    Organization organization =
        organizationRepository.save(
            Organization.builder()
                .name(request.name())
                .industry(request.industry())
                .contactEmail(request.contactEmail())
                .contactPhone(request.contactPhone())
                .address(request.address())
                .website(request.website())
                .positions(request.positions())
                .active(request.activeOrDefault())
                .build());
    return OrganizationResponse.from(organization);
  }

  @Transactional
  public OrganizationResponse update(UUID id, OrganizationRequest request) {
    Organization organization = find(id);

    if (!organization.getName().equals(request.name())
        && organizationRepository.existsByName(request.name())) {
      throw new BusinessException(DUPLICATE_ORGANIZATION, "Organization name already exists.");
    }

    organization.update(
        request.name(),
        request.industry(),
        request.contactEmail(),
        request.contactPhone(),
        request.address(),
        request.website(),
        request.positions(),
        request.activeOrDefault());
    return OrganizationResponse.from(organizationRepository.save(organization));
  }

  @Transactional
  public void delete(UUID id) {
    organizationRepository.delete(find(id));
  }

  private Organization find(UUID id) {
    return organizationRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ORGANIZATION_NOT_FOUND, "Organization not found: " + id));
  }
}
