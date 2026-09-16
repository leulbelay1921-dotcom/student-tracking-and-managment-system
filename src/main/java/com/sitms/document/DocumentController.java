package com.sitms.document;

import com.sitms.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Document upload/download endpoints (SITMS-037).
 *
 * <p>Upload requires ADMIN or STUDENT. Students upload to their own profile (self-service); admins
 * upload on behalf of a student via the request's studentId. Download is open to the owning
 * student, an admin, or a supervisor assigned to that student.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

  private final DocumentService documentService;

  public DocumentController(DocumentService documentService) {
    this.documentService = documentService;
  }

  /** Self-scoped list: students see only their own documents; supervisors their assigned students. */
  @GetMapping
  @PreAuthorize(
      "hasRole('ADMIN') or hasRole('STUDENT') or hasRole('SUPERVISOR') or hasRole('FACULTY')")
  public ApiResponse<ApiResponse.PageData<DocumentResponse>> list(
      Authentication authentication,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @PageableDefault(size = 20) Pageable pageable) {
    String role = principalRole(authentication);
    if ("STUDENT".equals(role)) {
      return ApiResponse.ok(
          ApiResponse.PageData.from(
              documentService.listStudentDocuments(
                  UUID.fromString(authentication.getName()), pageable)));
    }
    if ("SUPERVISOR".equals(role)) {
      return ApiResponse.ok(
          ApiResponse.PageData.from(
              documentService.listForSupervisor(
                  UUID.fromString(authentication.getName()), supervisorId, pageable)));
    }
    if ("FACULTY".equals(role)) {
      return ApiResponse.ok(
          ApiResponse.PageData.from(
              documentService.listForFaculty(
                  UUID.fromString(authentication.getName()), academicStaffId, pageable)));
    }
    return ApiResponse.ok(
        ApiResponse.PageData.from(documentService.listAll(pageable)));
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT')")
  public ApiResponse<DocumentResponse> upload(
      Authentication authentication,
      @RequestParam("file") MultipartFile file,
      @RequestParam(required = false) DocumentType type,
      @RequestParam(required = false) UUID internshipId,
      @RequestParam(required = false) UUID studentId) {
    boolean selfService =
        authentication.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_STUDENT"));
    UUID appUserId =
        selfService ? UUID.fromString(authentication.getName()) : null;
    UUID targetStudentId = selfService ? null : studentId;
    if (!selfService && targetStudentId == null) {
      throw new com.sitms.common.exception.BusinessException(
          DocumentService.STUDENT_PROFILE_NOT_FOUND, "A studentId is required for admin uploads.");
    }
    return ApiResponse.ok(
        "Document uploaded.",
        documentService.upload(file, type, internshipId, targetStudentId, appUserId, selfService));
  }

@GetMapping("/review-queue")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<ApiResponse.PageData<DocumentResponse>> reviewQueue(
      Authentication authentication,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId,
      @RequestParam(required = false) UUID studentId,
      @PageableDefault(size = 20) Pageable pageable) {
    String role = principalRole(authentication);
    UUID appUserId =
        "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        ApiResponse.PageData.from(
            documentService.reviewQueue(role, appUserId, supervisorId, academicStaffId, studentId, pageable)));
  }

@GetMapping("/{id}/download")
  public ResponseEntity<byte[]> download(
      Authentication authentication,
      @PathVariable UUID id,
      @RequestParam(required = false) UUID supervisorId,
      @RequestParam(required = false) UUID academicStaffId) {
    String role = principalRole(authentication);
    UUID appUserId =
        "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    DocumentService.DocumentContent content =
        documentService.download(id, appUserId, role, supervisorId, academicStaffId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(content.contentType()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename(content.fileName())
                .build()
                .toString())
        .body(content.bytes());
  }

  @PostMapping("/{id}/review")
  @PreAuthorize("hasRole('ADMIN') or hasRole('SUPERVISOR')")
  public ApiResponse<DocumentResponse> review(
      Authentication authentication,
      @PathVariable UUID id,
      @Valid @RequestBody DocumentReviewRequest request) {
    String role = principalRole(authentication);
    UUID appUserId =
        "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    return ApiResponse.ok(
        "Document reviewed.", documentService.review(id, request, role, appUserId));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN') or hasRole('STUDENT') or hasRole('SUPERVISOR')")
  public ApiResponse<Void> delete(
      Authentication authentication, @PathVariable UUID id) {
    String role = principalRole(authentication);
    UUID appUserId =
        "ADMIN".equals(role) ? null : UUID.fromString(authentication.getName());
    documentService.delete(id, role, appUserId);
    return ApiResponse.ok("Document deleted.", null);
  }

  private static String principalRole(Authentication authentication) {
    return authentication.getAuthorities().stream()
        .map(Object::toString)
        .map(a -> a.replace("ROLE_", ""))
        .filter(a -> a.equals("ADMIN") || a.equals("STUDENT") || a.equals("SUPERVISOR") || a.equals("FACULTY"))
        .findFirst()
        .orElseThrow(
            () ->
                new com.sitms.common.exception.BusinessException(
                    DocumentService.STUDENT_PROFILE_NOT_FOUND, "Unrecognized principal role."));
  }
}