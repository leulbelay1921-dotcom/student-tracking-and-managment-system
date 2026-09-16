package com.sitms.task;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Task CRUD + status endpoints for the company-supervisor workflow (SITMS-024).
 *
 * <p>Reads are role-scoped server-side: ADMIN sees all, SUPERVISOR sees tasks they created, STUDENT
 * sees their assigned tasks. Creating, editing and deleting tasks is reserved for the company
 * supervisor ({@code SUPERVISOR}); the supervisor id is always derived from the token and a task may
 * only be assigned to one of that supervisor's interns. Status transitions are split by role: the
 * assignee (STUDENT) starts and completes a task, the creating supervisor marks completed work as
 * reviewed.
 */
@RestController
@RequestMapping("/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<TaskResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID internshipId,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            taskService.list(
                role,
                appUserId,
                studentId,
                internshipId,
                supervisorId,
                status,
                priority,
                q,
                pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<TaskResponse> get(@PathVariable UUID id) {
    return ApiResponse.ok(taskService.get(id));
  }

  @GetMapping("/progress")
  public ApiResponse<TaskProgress> progress(
      JwtAuthenticationToken token,
      @RequestParam(required = false) UUID studentId,
      @RequestParam(required = false) UUID internshipId) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(taskService.progressFor(role, appUserId, internshipId, studentId));
  }

  @PostMapping
  @PreAuthorize("hasRole('SUPERVISOR')")
  public ApiResponse<TaskResponse> create(
      JwtAuthenticationToken token, @Valid @RequestBody TaskRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Task created.",
        taskService.create(role, appUserId, request));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('SUPERVISOR')")
  public ApiResponse<TaskResponse> update(
      JwtAuthenticationToken token, @PathVariable UUID id, @Valid @RequestBody TaskRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Task updated.",
        taskService.update(id, role, appUserId, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('SUPERVISOR')")
  public ApiResponse<Void> delete(JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = PrincipalResolver.subject(token);
    taskService.delete(id, role, appUserId);
    return ApiResponse.ok("Task deleted.", null);
  }

  @PatchMapping("/{id}/status")
  @PreAuthorize("hasRole('SUPERVISOR') or hasRole('STUDENT')")
  public ApiResponse<TaskResponse> changeStatus(
      JwtAuthenticationToken token,
      @PathVariable UUID id,
      @Valid @RequestBody TaskStatusChangeRequest request) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = PrincipalResolver.subject(token);
    return ApiResponse.ok(
        "Task status updated.", taskService.changeStatus(id, role, appUserId, request.status()));
  }
}
