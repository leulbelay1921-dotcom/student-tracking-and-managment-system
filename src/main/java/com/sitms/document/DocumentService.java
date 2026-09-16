package com.sitms.document;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.academicstaff.AcademicStaff;
import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.Supervisor;
import com.sitms.supervisor.SupervisorRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Business logic for {@link Document} upload/download and validation (SITMS-037). */
@Service
public class DocumentService {

  public static final String DOCUMENT_NOT_FOUND = "DOCUMENT_NOT_FOUND";
  public static final String STUDENT_PROFILE_NOT_FOUND = "STUDENT_PROFILE_NOT_FOUND";
  public static final String SUPERVISOR_PROFILE_NOT_FOUND = "SUPERVISOR_PROFILE_NOT_FOUND";
  public static final String ACADEMIC_STAFF_PROFILE_NOT_FOUND = "ACADEMIC_STAFF_PROFILE_NOT_FOUND";
  public static final String INVALID_FILE_TYPE = "INVALID_FILE_TYPE";
  public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
  public static final String EMPTY_FILE = "EMPTY_FILE";
  public static final String INVALID_FILE_NAME = "INVALID_FILE_NAME";
  public static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";

  public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

  private static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of(
          "application/pdf",
          "application/msword",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "image/png",
          "image/jpeg",
          "text/plain");

  private final DocumentRepository documentRepository;
  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final AcademicStaffRepository academicStaffRepository;
  private final InternshipRepository internshipRepository;
  private final NotificationNotifier notifier;
  private final AuditLogService auditLogService;

  public DocumentService(
      DocumentRepository documentRepository,
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      AcademicStaffRepository academicStaffRepository,
      InternshipRepository internshipRepository,
      NotificationNotifier notifier,
      AuditLogService auditLogService) {
    this.documentRepository = documentRepository;
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.academicStaffRepository = academicStaffRepository;
    this.internshipRepository = internshipRepository;
    this.notifier = notifier;
    this.auditLogService = auditLogService;
  }

  /** Upload a file on behalf of a student. Students are forced to their own profile. */
  @Transactional
  public DocumentResponse upload(MultipartFile file, DocumentType type, UUID internshipId,
      UUID studentId, UUID appUserId, boolean selfService) {
    validate(file);
    UUID ownerId =
        selfService
            ? studentRepository
                .findByAppUserId(appUserId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            STUDENT_PROFILE_NOT_FOUND,
                            "No student profile for user: " + appUserId))
                .getId()
            : studentId;
    if (ownerId == null) {
      throw new BusinessException(STUDENT_PROFILE_NOT_FOUND, "A student uploader is required.");
    }
    DocumentType resolvedType = type == null ? DocumentType.OTHER : type;
    Document saved =
        documentRepository.saveAndFlush(
            Document.builder()
                .studentId(ownerId)
                .internshipId(internshipId)
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .size(file.getSize())
                .type(resolvedType)
                .status(DocumentStatus.PENDING)
                .content(readBytes(file))
                .build());
    notifier.notify(
        supervisorAppUserId(internshipId),
        NotificationType.DOCUMENT_UPLOADED,
        "Document awaiting review",
        saved.getFileName() == null ? "A new document was uploaded." : saved.getFileName() + " was uploaded.",
        "/documents");
    auditLogService.record(
        AuditAction.DOCUMENT_UPLOADED,
        "DOCUMENT",
        saved.getId(),
        "Document " + (saved.getFileName() == null ? "" : "'" + saved.getFileName() + "' ") + "uploaded.");
    return DocumentResponse.from(saved);
  }

  private static byte[] readBytes(MultipartFile file) {
    try {
      return file.getBytes();
    } catch (java.io.IOException e) {
      throw new BusinessException(INVALID_FILE_TYPE, "The uploaded file could not be read.");
    }
  }

  /** List documents, scoped to a student's own profile by the POST /documents uploader. */
  @Transactional(readOnly = true)
  public Page<DocumentResponse> listStudentDocuments(UUID appUserId, Pageable pageable) {
    UUID studentId =
        studentRepository
            .findByAppUserId(appUserId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        STUDENT_PROFILE_NOT_FOUND,
                        "No student profile for user: " + appUserId))
            .getId();
    return documentRepository.findByStudentId(studentId, pageable).map(DocumentResponse::from);
  }

  /** List all documents (admin scope). */
  @Transactional(readOnly = true)
  public Page<DocumentResponse> listAll(Pageable pageable) {
    return documentRepository.findAll(pageable).map(DocumentResponse::from);
  }

  /** List documents of students assigned to the calling supervisor. */
  @Transactional(readOnly = true)
  public Page<DocumentResponse> listForSupervisor(UUID appUserId, Pageable pageable) {
    return listForSupervisor(appUserId, null, pageable);
  }

  /** List documents of students assigned to a supervisor profile (or the caller's own). */
  @Transactional(readOnly = true)
  public Page<DocumentResponse> listForSupervisor(
      UUID appUserId, UUID supervisorId, Pageable pageable) {
    UUID target = supervisorId != null ? supervisorId : resolveSupervisorId(appUserId);
    return documentRepository
        .findByAssignedSupervisor(target, pageable)
        .map(DocumentResponse::from);
  }

  /** List documents for an academic staff member, or of students assigned to a picked staff member. */
  @Transactional(readOnly = true)
  public Page<DocumentResponse> listForFaculty(
      UUID appUserId, UUID academicStaffId, Pageable pageable) {
    UUID target =
        academicStaffId != null ? academicStaffId : resolveAcademicStaffId(appUserId);
    return documentRepository
        .findByAssignedAcademicStaff(target, pageable)
        .map(DocumentResponse::from);
  }

  /**
   * PENDING documents: supervisors see their assigned students' documents, admins see all.
   * Optional supervisorId/academicStaffId overrides scope (same role only), and studentId
   * narrows to a single student.
   */
  @Transactional(readOnly = true)
  public Page<DocumentResponse> reviewQueue(
      String role, UUID appUserId, UUID supervisorId, UUID academicStaffId, UUID studentId, Pageable pageable) {
    if (studentId != null) {
      return documentRepository
          .findByStudentIdAndStatus(studentId, DocumentStatus.PENDING, pageable)
          .map(DocumentResponse::from);
    }
    if ("SUPERVISOR".equals(role)) {
      UUID resolvedSupervisorId = supervisorId != null ? supervisorId : resolveSupervisorId(appUserId);
      return documentRepository
          .findByStatusAndAssignedSupervisor(DocumentStatus.PENDING, resolvedSupervisorId, pageable)
          .map(DocumentResponse::from);
    }
    if (supervisorId != null) {
      return documentRepository
          .findByStatusAndAssignedSupervisor(DocumentStatus.PENDING, supervisorId, pageable)
          .map(DocumentResponse::from);
    }
    if (academicStaffId != null) {
      return documentRepository
          .findByStatusAndAssignedAcademicStaff(DocumentStatus.PENDING, academicStaffId, pageable)
          .map(DocumentResponse::from);
    }
    return documentRepository.findByStatus(DocumentStatus.PENDING, pageable).map(DocumentResponse::from);
  }

  /** Reviewer decision on a PENDING document: approve or reject with optional feedback. */
  @Transactional
  public DocumentResponse review(
      UUID id, DocumentReviewRequest request, String role, UUID appUserId) {
    Document document = find(id);
    if (document.getStatus() != DocumentStatus.PENDING) {
      throw new BusinessException(
          INVALID_STATUS_TRANSITION,
          "Only pending documents can be reviewed: " + document.getStatus());
    }
    UUID reviewerId = null;
    if ("SUPERVISOR".equals(role)) {
      reviewerId = resolveSupervisorId(appUserId);
      if (!internshipRepository.existsBySupervisorIdAndStudentId(
          reviewerId, document.getStudentId())) {
        throw new AccessDeniedException(
            "Document is not assigned to this supervisor: " + id);
      }
    }
    document.updateReview(
        reviewerId,
        request.decision() == DocumentStatus.APPROVED
            ? DocumentStatus.APPROVED
            : DocumentStatus.REJECTED,
        request.feedback());
    DocumentResponse response =
        DocumentResponse.from(documentRepository.saveAndFlush(document));
    notifier.notify(
        studentAppUserId(document.getStudentId()),
        NotificationType.DOCUMENT_REVIEWED,
        request.decision() == DocumentStatus.APPROVED
            ? "Document approved"
            : "Document rejected",
        request.feedback() == null ? null : "Feedback: " + request.feedback(),
        "/documents");
    auditLogService.record(
        AuditAction.DOCUMENT_REVIEWED,
        "DOCUMENT",
        document.getId(),
        "Document " + (request.decision() == DocumentStatus.APPROVED ? "approved" : "rejected") + ".");
    return response;
  }

  private UUID resolveSupervisorId(UUID appUserId) {
    return supervisorRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    SUPERVISOR_PROFILE_NOT_FOUND,
                    "No supervisor profile for user: " + appUserId))
        .getId();
  }

  private UUID resolveAcademicStaffId(UUID appUserId) {
    return academicStaffRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ACADEMIC_STAFF_PROFILE_NOT_FOUND,
                    "No academic staff profile for user: " + appUserId))
        .getId();
  }

  /** The app_user id of the supervisor of the internship that owns the document, or null. */
  private UUID supervisorAppUserId(UUID internshipId) {
    if (internshipId == null) {
      return null;
    }
    return internshipRepository
        .findById(internshipId)
        .flatMap(internship -> supervisorRepository.findById(internship.getSupervisorId()))
        .map(Supervisor::getAppUserId)
        .orElse(null);
  }

  /** The app_user id of the student profile, or null. */
  private UUID studentAppUserId(UUID studentId) {
    return studentRepository.findById(studentId).map(Student::getAppUserId).orElse(null);
  }

  /**
   * Download a document's raw bytes. The owning student, an admin, or a supervisor assigned to
   * that student may access it.
   */
  @Transactional(readOnly = true)
  public DocumentContent download(UUID id, UUID appUserId, String role) {
    return download(id, appUserId, role, null, null);
  }

  /** Download a document's raw bytes, with an optional supervisor target for scoped viewers. */
  @Transactional(readOnly = true)
  public DocumentContent download(
      UUID id, UUID appUserId, String role, UUID supervisorId, UUID academicStaffId) {
    Document document = find(id);
    if (!hasDownloadAccess(document, appUserId, role, supervisorId, academicStaffId)) {
      throw new AccessDeniedException("Document is not accessible to this user: " + id);
    }
    return new DocumentContent(document.getContent(), document.getContentType(), document.getFileName());
  }

  private boolean hasDownloadAccess(
      Document document, UUID appUserId, String role, UUID targetSupervisorId, UUID targetAcademicStaffId) {
    if ("ADMIN".equals(role)) {
      return true;
    }
    if ("STUDENT".equals(role)) {
      UUID studentId =
          studentRepository
              .findByAppUserId(appUserId)
              .map(com.sitms.student.Student::getId)
              .orElse(null);
      return studentId != null && studentId.equals(document.getStudentId());
    }
    if ("SUPERVISOR".equals(role)) {
      UUID supervisorId = supervisorRepository
              .findByAppUserId(appUserId)
              .map(com.sitms.supervisor.Supervisor::getId)
              .orElse(null);
      return supervisorId != null
          && internshipRepository.existsBySupervisorIdAndStudentId(
              supervisorId, document.getStudentId());
    }
    if ("FACULTY".equals(role)) {
      if (targetAcademicStaffId != null) {
        return internshipRepository.existsByAcademicStaffIdAndStudentId(
            targetAcademicStaffId, document.getStudentId());
      }
      UUID academicStaffId =
          academicStaffRepository
              .findByAppUserId(appUserId)
              .map(AcademicStaff::getId)
              .orElse(null);
      return academicStaffId != null
          && internshipRepository.existsByAcademicStaffIdAndStudentId(
              academicStaffId, document.getStudentId());
    }
    return false;
  }

  /**
   * Delete a document. Admins may delete any document; students may delete their own. Supervisors
   * may delete documents of students assigned to them.
   */
  @Transactional
  public void delete(UUID id, String role, UUID appUserId) {
    Document document = find(id);
    if (!hasDeleteAccess(document, role, appUserId)) {
      throw new AccessDeniedException("Document is not deletable by this user: " + id);
    }
    documentRepository.delete(document);
    auditLogService.record(
        AuditAction.DOCUMENT_DELETED,
        "DOCUMENT",
        document.getId(),
        "Document '"
            + (document.getFileName() == null ? "" : document.getFileName())
            + "' deleted.");
  }

  private boolean hasDeleteAccess(Document document, String role, UUID appUserId) {
    if ("ADMIN".equals(role)) {
      return true;
    }
    if ("STUDENT".equals(role)) {
      UUID studentId =
          studentRepository
              .findByAppUserId(appUserId)
              .map(com.sitms.student.Student::getId)
              .orElse(null);
      return studentId != null && studentId.equals(document.getStudentId());
    }
    if ("SUPERVISOR".equals(role)) {
      UUID supervisorId =
          supervisorRepository
              .findByAppUserId(appUserId)
              .map(com.sitms.supervisor.Supervisor::getId)
              .orElse(null);
      return supervisorId != null
          && internshipRepository.existsBySupervisorIdAndStudentId(
              supervisorId, document.getStudentId());
    }
    return false;
  }

  private void validate(MultipartFile file) {
    if (file.isEmpty()) {
      throw new BusinessException(EMPTY_FILE, "The uploaded file is empty.");
    }
    if (file.getSize() > MAX_FILE_SIZE) {
      throw new BusinessException(FILE_TOO_LARGE, "File exceeds the 10 MB limit.");
    }
    String contentType = file.getContentType();
    if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new BusinessException(
          INVALID_FILE_TYPE, "File type not allowed: " + contentType);
    }
    String fileName = file.getOriginalFilename();
    if (fileName == null || fileName.isBlank()) {
      throw new BusinessException(INVALID_FILE_NAME, "The uploaded file must have a name.");
    }
  }

  private Document find(UUID id) {
    return documentRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException(DOCUMENT_NOT_FOUND, "Document not found: " + id));
  }

  /** Raw document payload for the download endpoint. */
  public record DocumentContent(byte[] bytes, String contentType, String fileName) {}
}