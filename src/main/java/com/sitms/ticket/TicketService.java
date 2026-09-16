package com.sitms.ticket;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.StudentRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business logic for student support {@link Ticket tickets} (SITMS-073). */
@Service
public class TicketService {

  public static final String TICKET_NOT_FOUND = "TICKET_NOT_FOUND";
  public static final String INVALID_TICKET_TRANSITION = "INVALID_TICKET_TRANSITION";
  public static final String INVALID_TICKET_ASSIGNMENT = "INVALID_TICKET_ASSIGNMENT";
  public static final String INTERNSHIP_NOT_FOUND = "INTERNSHIP_NOT_FOUND";

  private final TicketRepository ticketRepository;
  private final InternshipRepository internshipRepository;
  private final StudentRepository studentRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;
  private final DataScope dataScope;

  public TicketService(
      TicketRepository ticketRepository,
      InternshipRepository internshipRepository,
      StudentRepository studentRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService,
      DataScope dataScope) {
    this.ticketRepository = ticketRepository;
    this.internshipRepository = internshipRepository;
    this.studentRepository = studentRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
    this.dataScope = dataScope;
  }

  /**
   * Lists tickets scoped to the principal. STUDENT sees their own; SUPERVISOR (and FACULTY) the
   * students assigned to them; ADMIN all. Cross-scope filters are silently empty.
   */
  @Transactional(readOnly = true)
  public Page<TicketResponse> list(
      PrincipalRole role,
      UUID appUserId,
      UUID studentId,
      UUID internshipId,
      TicketStatus status,
      Pageable pageable) {
    UUID scopeStudentId = studentId;
    UUID scopeSupervisorId = null;
    UUID scopeAcademicStaffId = null;
    switch (role) {
      case STUDENT -> {
        UUID own = dataScope.studentId(appUserId);
        scopeStudentId = own;
        if (studentId != null && !studentId.equals(own)) {
          return Page.empty(pageable);
        }
      }
      case SUPERVISOR -> {
        scopeSupervisorId = dataScope.supervisorId(appUserId);
        if (studentId != null && !dataScope.isAssignedToStudent(appUserId, studentId)) {
          return Page.empty(pageable);
        }
      }
      case FACULTY -> {
        scopeAcademicStaffId = dataScope.academicStaffId(appUserId);
        if (studentId != null && !dataScope.isAssignedToAcademicStudent(appUserId, studentId)) {
          return Page.empty(pageable);
        }
      }
      case ADMIN -> {
        // no extra scoping
      }
    }
    return ticketRepository
        .findAll(filters(scopeStudentId, internshipId, scopeSupervisorId, scopeAcademicStaffId, status), pageable)
        .map(TicketResponse::from);
  }

  private static Specification<Ticket> filters(
      UUID studentId,
      UUID internshipId,
      UUID supervisorId,
      UUID academicStaffId,
      TicketStatus status) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (studentId != null) {
        predicates.add(cb.equal(root.get("studentId"), studentId));
      }
      if (internshipId != null) {
        predicates.add(cb.equal(root.get("internshipId"), internshipId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (supervisorId != null) {
        jakarta.persistence.criteria.Subquery<UUID> sub = query.subquery(UUID.class);
        var internshipRoot = sub.from(Internship.class);
        sub.select(internshipRoot.get("studentId"))
            .where(cb.equal(internshipRoot.get("supervisorId"), supervisorId));
        predicates.add(root.get("studentId").in(sub));
      }
      if (academicStaffId != null) {
        jakarta.persistence.criteria.Subquery<UUID> sub = query.subquery(UUID.class);
        var internshipRoot = sub.from(Internship.class);
        sub.select(internshipRoot.get("studentId"))
            .where(cb.equal(internshipRoot.get("academicStaffId"), academicStaffId));
        predicates.add(root.get("studentId").in(sub));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /** Fetches a single ticket, verifying the principal may see it. Cross-scope get returns 404. */
  @Transactional(readOnly = true)
  public TicketResponse get(UUID id, PrincipalRole role, UUID appUserId) {
    Ticket ticket = find(id);
    if (role == PrincipalRole.STUDENT
        && !ticket.getStudentId().equals(dataScope.studentId(appUserId))) {
      throw new ResourceNotFoundException(TICKET_NOT_FOUND, "Ticket not found: " + id);
    }
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, ticket.getStudentId())) {
      throw new ResourceNotFoundException(TICKET_NOT_FOUND, "Ticket not found: " + id);
    }
    if (role == PrincipalRole.FACULTY
        && !dataScope.isAssignedToAcademicStudent(appUserId, ticket.getStudentId())) {
      throw new ResourceNotFoundException(TICKET_NOT_FOUND, "Ticket not found: " + id);
    }
    return TicketResponse.from(ticket);
  }

  /** Raises a new ticket for a student's internship. */
  @Transactional
  public TicketResponse create(PrincipalRole role, UUID appUserId, TicketRequest request) {
    Internship internship =
        internshipRepository
            .findById(request.internshipId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        INTERNSHIP_NOT_FOUND, "Internship not found: " + request.internshipId()));
    UUID studentId = internship.getStudentId();
    if (role == PrincipalRole.STUDENT
        && !studentId.equals(dataScope.studentId(appUserId))) {
      throw new AccessDeniedException("Tickets can only be raised for your own internship.");
    }
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, studentId)) {
      throw new AccessDeniedException("Student is not assigned to this supervisor: " + studentId);
    }
    if (role == PrincipalRole.FACULTY
        && !dataScope.isAssignedToAcademicStudent(appUserId, studentId)) {
      throw new AccessDeniedException(
          "Student is not assigned to this academic staff member: " + studentId);
    }
    TicketAssigneeType assigneeType = null;
    UUID assigneeId = null;
    if (role == PrincipalRole.SUPERVISOR) {
      assigneeType = TicketAssigneeType.SUPERVISOR;
      assigneeId = dataScope.supervisorId(appUserId);
    } else if (role == PrincipalRole.FACULTY) {
      assigneeType = TicketAssigneeType.FACULTY;
      assigneeId = dataScope.academicStaffId(appUserId);
    }
    Ticket ticket =
        ticketRepository.save(
            Ticket.builder()
                .studentId(studentId)
                .internshipId(internship.getId())
                .title(request.title())
                .description(request.description())
                .priority(request.priorityOrDefault())
                .status(TicketStatus.OPEN)
                .assigneeType(assigneeType)
                .assigneeId(assigneeId)
                .build());
    TicketResponse response = TicketResponse.from(ticket);
    notifier.notify(
        studentAppUserId(studentId),
        NotificationType.TICKET_CREATED,
        "New support ticket",
        "Your ticket \"" + ticket.getTitle() + "\" was raised.",
        "/internships");
    auditLogService.record(
        AuditAction.TICKET_CREATED,
        "TICKET",
        ticket.getId(),
        "Ticket created: " + ticket.getTitle());
    return response;
  }

  /**
   * Advances a non-terminal ticket's status and/or (re)assigns it. Only assigned supervisors,
   * faculty members, or admins may update a ticket.
   */
  @Transactional
  public TicketResponse update(
      UUID id, PrincipalRole role, UUID appUserId, TicketUpdateRequest request) {
    Ticket ticket = find(id);
    if (role == PrincipalRole.SUPERVISOR
        && !dataScope.isAssignedToStudent(appUserId, ticket.getStudentId())) {
      throw new AccessDeniedException("Student is not assigned to this supervisor.");
    }
    if (role == PrincipalRole.FACULTY
        && !dataScope.isAssignedToAcademicStudent(appUserId, ticket.getStudentId())) {
      throw new AccessDeniedException(
          "Student is not assigned to this academic staff member.");
    }
    if (ticket.isTerminal()) {
      throw new BusinessException(
          INVALID_TICKET_TRANSITION, "A resolved or closed ticket cannot be modified.");
    }
    TicketStatus status = request.status() == null ? ticket.getStatus() : request.status();
    if (request.status() != null
        && !request.status().equals(ticket.getStatus())
        && !canTransition(ticket.getStatus(), status)) {
      throw new BusinessException(
          INVALID_TICKET_TRANSITION,
          "Cannot move ticket " + ticket.getStatus() + " -> " + status);
    }
    TicketAssigneeType assigneeType = request.assigneeType();
    UUID assigneeId = request.assigneeId();
    if ((assigneeType == null) != (assigneeId == null)) {
      throw new BusinessException(
          INVALID_TICKET_ASSIGNMENT, "Assignee type and id must both be set, or both be null.");
    }
    if (assigneeType == null) {
      if (role == PrincipalRole.SUPERVISOR) {
        assigneeType = TicketAssigneeType.SUPERVISOR;
        assigneeId = dataScope.supervisorId(appUserId);
      } else {
        assigneeType = ticket.getAssigneeType();
        assigneeId = ticket.getAssigneeId();
      }
    }
    if (role == PrincipalRole.SUPERVISOR
        && (assigneeType != TicketAssigneeType.SUPERVISOR
            || !assigneeId.equals(dataScope.supervisorId(appUserId)))) {
      throw new AccessDeniedException("Company supervisors may only assign tickets to themselves.");
    }
    ticket.update(status, assigneeType, assigneeId);
    TicketResponse response = TicketResponse.from(ticketRepository.save(ticket));
    notifier.notify(
        studentAppUserId(ticket.getStudentId()),
        NotificationType.TICKET_UPDATED,
        "Support ticket updated",
        "Your ticket \"" + ticket.getTitle() + "\" is now " + status + ".",
        "/internships");
    auditLogService.record(
        AuditAction.TICKET_UPDATED,
        "TICKET",
        ticket.getId(),
        "Ticket " + ticket.getTitle() + " moved to " + status + ".");
    return response;
  }

  private static boolean canTransition(TicketStatus current, TicketStatus target) {
    if (current == target) {
      return true;
    }
    return switch (current) {
      case OPEN -> target == TicketStatus.IN_PROGRESS
          || target == TicketStatus.RESOLVED
          || target == TicketStatus.CLOSED;
      case IN_PROGRESS -> target == TicketStatus.RESOLVED
          || target == TicketStatus.CLOSED;
      case RESOLVED -> target == TicketStatus.CLOSED;
      case CLOSED -> false;
    };
  }

  private Ticket find(UUID id) {
    return ticketRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(TICKET_NOT_FOUND, "Ticket not found: " + id));
  }

  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(s -> s.getAppUserId()).orElse(null);
  }
}