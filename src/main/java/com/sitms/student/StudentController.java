package com.sitms.student;

import com.sitms.common.response.ApiResponse;
import com.sitms.common.security.PrincipalResolver;
import com.sitms.common.security.PrincipalRole;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Student CRUD endpoints (SITMS-007).
 *
 * <p>Reads are open to ADMIN / SUPERVISOR / FACULTY / STUDENT (mirroring {@code /students/**} in
 * {@code SecurityConfig}); writes are ADMIN-only.
 */
@RestController
@RequestMapping("/students")
public class StudentController {

  private final StudentService studentService;

  public StudentController(StudentService studentService) {
    this.studentService = studentService;
  }

  @GetMapping
  public ApiResponse<ApiResponse.PageData<StudentResponse>> list(
      JwtAuthenticationToken token,
      @RequestParam(required = false) String program,
      @RequestParam(required = false) String department,
      @RequestParam(required = false) StudentStatus status,
      @RequestParam(required = false) Integer batch,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) String q,
      @PageableDefault(size = 20) Pageable pageable) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            studentService.list(
                role, appUserId, supervisorId, academicStaffId, q, program, department, status, batch, pageable)));
  }

  @GetMapping("/{id}")
  public ApiResponse<StudentResponse> get(JwtAuthenticationToken token, @PathVariable UUID id) {
    PrincipalRole role = PrincipalResolver.roleOf(token);
    UUID appUserId = role != PrincipalRole.ADMIN ? PrincipalResolver.subject(token) : null;
    return ApiResponse.ok(studentService.get(role, appUserId, id));
  }

  @GetMapping("/me")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<StudentResponse> getMine(JwtAuthenticationToken token) {
    return ApiResponse.ok(studentService.getMine(subject(token)));
  }

  @PutMapping("/me")
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<StudentResponse> updateMine(
      JwtAuthenticationToken token, @Valid @RequestBody StudentProfileUpdateRequest request) {
    return ApiResponse.ok("Profile updated.", studentService.updateMine(subject(token), request));
  }

  @PostMapping(value = "/me/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('STUDENT')")
  public ApiResponse<StudentResponse> uploadMinePhoto(
      JwtAuthenticationToken token, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok("Profile photo updated.", studentService.uploadMinePhoto(subject(token), file));
  }

  @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<StudentResponse> uploadPhoto(
      @PathVariable UUID id, @RequestParam("file") MultipartFile file) {
    return ApiResponse.ok("Profile photo updated.", studentService.uploadPhoto(id, file));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentRequest request) {
    return ApiResponse.ok("Student created.", studentService.create(request));
  }

  private UUID subject(JwtAuthenticationToken token) {
    return UUID.fromString(token.getToken().getSubject());
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<StudentResponse> update(
      @PathVariable UUID id, @Valid @RequestBody StudentRequest request) {
    return ApiResponse.ok("Student updated.", studentService.update(id, request));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<Void> delete(@PathVariable UUID id) {
    studentService.delete(id);
    return ApiResponse.ok("Student deleted.", null);
  }
}
