package com.sitms.task;

import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.DataScope;
import com.sitms.common.security.PrincipalRole;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.SupervisorRepository;
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

/** Business logic for {@link Task} and its status lifecycle (SITMS-024). */
@Service
public class TaskService {

  public static final String TASK_NOT_FOUND = "TASK_NOT_FOUND";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String SUPERVISOR_PROFILE_NOT_FOUND = "SUPERVISOR_PROFILE_NOT_FOUND";
  public static final String STUDENT_NOT_ASSIGNED = "STUDENT_NOT_ASSIGNED";
  public static final String TASK_NOT_OWNED = "TASK_NOT_OWNED";
  public static final String COMPLETED_TASK_DELETION = "COMPLETED_TASK_DELETION";

  private final TaskRepository taskRepository;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final DataScope dataScope;

  public TaskService(
      TaskRepository taskRepository,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      DataScope dataScope) {
    this.taskRepository = taskRepository;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.dataScope = dataScope;
  }

  /**
   * Lists tasks filtered server-side by the given criteria and scoped to the calling principal.
   *
   * <p>ADMIN sees all tasks; SUPERVISOR sees only tasks they own; STUDENT sees only their own
   * tasks. Any client-supplied filters are applied on top of the scope.
   */
  public Page<TaskResponse> list(
      PrincipalRole scope,
      UUID appUserId,
      UUID studentId,
      UUID internshipId,
      UUID supervisorId,
      TaskStatus status,
      TaskPriority priority,
      String q,
      Pageable pageable) {
    Page<Task> scoped;
    if (scope == PrincipalRole.STUDENT) {
      UUID own = ownStudentId(studentRepository, appUserId);
      scoped = taskRepository.findAll(filters(own, internshipId, null, status, priority, q), pageable);
    } else if (scope == PrincipalRole.SUPERVISOR) {
      UUID own = ownSupervisorId(supervisorRepository, appUserId);
      scoped = taskRepository.findAll(filters(null, internshipId, own, status, priority, q), pageable);
    } else {
      scoped =
          taskRepository.findAll(
              filters(studentId, internshipId, supervisorId, status, priority, q), pageable);
    }
    return scoped.map(TaskResponse::from);
  }

  /** Builds an AND-composed predicate from the optional filters; empty returns match-all. */
  private static Specification<Task> filters(
      UUID studentId,
      UUID internshipId,
      UUID supervisorId,
      TaskStatus status,
      TaskPriority priority,
      String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (studentId != null) {
        predicates.add(cb.equal(root.get("studentId"), studentId));
      }
      if (internshipId != null) {
        predicates.add(cb.equal(root.get("internshipId"), internshipId));
      }
      if (supervisorId != null) {
        predicates.add(cb.equal(root.get("supervisorId"), supervisorId));
      }
      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }
      if (priority != null) {
        predicates.add(cb.equal(root.get("priority"), priority));
      }
      if (q != null && !q.isBlank()) {
        String like = "%" + q.trim().toLowerCase() + "%";
        Predicate title = cb.like(cb.lower(root.get("title")), like);
        Predicate description = cb.like(cb.lower(root.get("description")), like);
        predicates.add(cb.or(title, description));
      }
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  public TaskResponse get(UUID id) {
    return TaskResponse.from(find(id));
  }

  /**
   * Creates a task on behalf of the calling (company) supervisor, derived from the token rather than
   * the request. The task can only be assigned to one of the supervisor's interns; legal status is
   * {@link TaskStatus#ASSIGNED}.
   */
  @Transactional
  public TaskResponse create(PrincipalRole role, UUID appUserId, TaskRequest request) {
    requireSupervisor(role);
    UUID supervisorId = dataScope.supervisorId(appUserId);
    validateAssignment(appUserId, request.studentId());
    validateStudent(request.studentId());
    Task task =
        taskRepository.saveAndFlush(
            Task.builder()
                .studentId(request.studentId())
                .internshipId(request.internshipId())
                .supervisorId(supervisorId)
                .title(request.title())
                .description(request.description())
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .category(request.category())
                .expectedOutcome(request.expectedOutcome())
                .status(TaskStatus.ASSIGNED)
                .priority(defaultPriority(request))
                .build());
    return TaskResponse.from(task);
  }

  /**
   * Updates a task. Only the supervisor who created it may do so, and only their supervisor identity
   * is allowed to change the assignee — again restricted to students assigned to them.
   */
  @Transactional
  public TaskResponse update(
      UUID id, PrincipalRole role, UUID appUserId, TaskRequest request) {
    requireSupervisor(role);
    Task task = find(id);
    UUID supervisorId = dataScope.supervisorId(appUserId);
    if (!task.getSupervisorId().equals(supervisorId)) {
      throw new AccessDeniedException(
          "Only the supervisor who created a task may edit it: " + id);
    }
    validateAssignment(appUserId, request.studentId());
    validateStudent(request.studentId());
    task.update(
        request.studentId(),
        request.internshipId(),
        request.title(),
        request.description(),
        request.startDate(),
        request.dueDate(),
        defaultPriority(request),
        request.category(),
        request.expectedOutcome());
    return TaskResponse.from(taskRepository.save(task));
  }

  private void requireSupervisor(PrincipalRole role) {
    if (role != PrincipalRole.SUPERVISOR) {
      throw new AccessDeniedException("Only a company supervisor may manage tasks.");
    }
  }

  private void validateStudent(UUID studentId) {
    if (!studentRepository.existsById(studentId)) {
      throw new ResourceNotFoundException(
          STUDENT_PROFILE_NOT_FOUND, "No student profile: " + studentId);
    }
  }

  /** A supervisor may only assign tasks to students assigned to them via an internship. */
  private void validateAssignment(UUID appUserId, UUID studentId) {
    if (!dataScope.isAssignedToStudent(appUserId, studentId)) {
      throw new AccessDeniedException(
          "Student is not assigned to this supervisor: " + studentId);
    }
  }

  /**
   * Deletes a task. Only the creating supervisor may delete, and only while the task is not
   * completed or reviewed.
   */
  @Transactional
  public void delete(UUID id, PrincipalRole role, UUID appUserId) {
    requireSupervisor(role);
    Task task = find(id);
    UUID supervisorId = dataScope.supervisorId(appUserId);
    if (!task.getSupervisorId().equals(supervisorId)) {
      throw new AccessDeniedException("Only the supervisor who created a task may delete it: " + id);
    }
    if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.REVIEWED) {
      throw new BusinessException(
          COMPLETED_TASK_DELETION,
          "A completed or reviewed task cannot be deleted: " + id);
    }
    taskRepository.delete(task);
  }

  /**
   * Transitions a task's status, verifying the principal may drive that transition.
   *
   * <p>STUDENT may start ({@code ASSIGNED -> IN_PROGRESS}) and submit ({@code ASSIGNED /
   * IN_PROGRESS -> COMPLETED}) only for their own tasks; SUPERVISOR may mark one of their own
   * completed tasks as {@code REVIEWED}. COMPLETED and REVIEWED are terminal.
   */
  @Transactional
  public TaskResponse changeStatus(UUID id, PrincipalRole role, UUID appUserId, TaskStatus target) {
    Task task = find(id);
    TaskStatus current = task.getStatus();
    if (role == PrincipalRole.STUDENT) {
      UUID own = ownStudentId(studentRepository, appUserId);
      if (!task.getStudentId().equals(own)) {
        throw new AccessDeniedException("Tasks may only be updated by their assignee: " + id);
      }
      if (target != TaskStatus.IN_PROGRESS && target != TaskStatus.COMPLETED) {
        throw new AccessDeniedException("A student may only start or complete a task.");
      }
    } else if (role == PrincipalRole.SUPERVISOR) {
      UUID own = ownSupervisorId(supervisorRepository, appUserId);
      if (!task.getSupervisorId().equals(own)) {
        throw new AccessDeniedException("Tasks may only be reviewed by their creator: " + id);
      }
      if (target != TaskStatus.REVIEWED) {
        throw new AccessDeniedException("A supervisor may only mark a task as reviewed.");
      }
    } else {
      throw new AccessDeniedException("This role may not update task status.");
    }
    if (!canTransition(current, target)) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION, "Cannot move " + current + " -> " + target);
    }
    task.changeStatus(target);
    return TaskResponse.from(taskRepository.save(task));
  }

  private static TaskPriority defaultPriority(TaskRequest request) {
    return request.priority() != null ? request.priority() : TaskPriority.MEDIUM;
  }

  private static boolean canTransition(TaskStatus current, TaskStatus target) {
    if (current == target) {
      return true;
    }
    return switch (current) {
      case ASSIGNED, PENDING ->
          target == TaskStatus.IN_PROGRESS || target == TaskStatus.COMPLETED;
      case IN_PROGRESS -> target == TaskStatus.COMPLETED;
      case COMPLETED -> target == TaskStatus.REVIEWED;
      default -> false;
    };
  }

  /**
   * Computes task progress for a student, verifying the principal may view it. STUDENT is forced to
   * their own profile; SUPERVISOR only their assigned students; ADMIN any.
   */
  public TaskProgress progressFor(
      PrincipalRole role, UUID appUserId, UUID internshipId, UUID studentId) {
    if (role == PrincipalRole.STUDENT) {
      UUID own = ownStudentId(studentRepository, appUserId);
      if (studentId != null && !studentId.equals(own)) {
        throw new AccessDeniedException("Progress is only available for your own profile.");
      }
      return progress(internshipId, own);
    }
    if (role == PrincipalRole.SUPERVISOR
        && studentId != null
        && !dataScope.isAssignedToStudent(appUserId, studentId)) {
      throw new AccessDeniedException("Student is not assigned to this supervisor: " + studentId);
    }
    return progress(internshipId, studentId);
  }

  /**
   * Computes task progress for a student (and optionally an internship).
   *
   * <p>When {@code internshipId} is provided it scopes the counts to that internship; otherwise to
   * the whole student.
   */
  public TaskProgress progress(UUID internshipId, UUID studentId) {
    if (internshipId == null && studentId == null) {
      return TaskProgress.of(null, null, 0, 0, 0, 0);
    }
    long total;
    long completed;
    long inProgress;
    if (internshipId != null) {
      total = taskRepository.countByInternshipId(internshipId);
      completed = taskRepository.countByInternshipIdAndStatus(internshipId, TaskStatus.COMPLETED);
      inProgress =
          taskRepository.countByInternshipIdAndStatus(internshipId, TaskStatus.IN_PROGRESS);
    } else {
      total = taskRepository.countByStudentId(studentId);
      completed = taskRepository.countByStudentIdAndStatus(studentId, TaskStatus.COMPLETED);
      inProgress = taskRepository.countByStudentIdAndStatus(studentId, TaskStatus.IN_PROGRESS);
    }
    long pending = total - completed - inProgress;
    return TaskProgress.of(studentId, internshipId, total, completed, inProgress, pending);
  }

  private Task find(UUID id) {
    return taskRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException(TASK_NOT_FOUND, "Task not found: " + id));
  }

  private static UUID ownStudentId(StudentRepository students, UUID appUserId) {
    return students
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    STUDENT_PROFILE_NOT_FOUND, "No student profile for user: " + appUserId))
        .getId();
  }

  private static UUID ownSupervisorId(SupervisorRepository supervisors, UUID appUserId) {
    return supervisors
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    SUPERVISOR_PROFILE_NOT_FOUND, "No supervisor profile for user: " + appUserId))
        .getId();
  }
}
