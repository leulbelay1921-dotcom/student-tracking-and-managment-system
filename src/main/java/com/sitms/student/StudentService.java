package com.sitms.student;

import com.sitms.academicstaff.AcademicStaffRepository;
import com.sitms.common.exception.BusinessException;
import com.sitms.common.exception.ResourceNotFoundException;
import com.sitms.common.security.PrincipalRole;
import com.sitms.internship.Internship;
import com.sitms.internship.InternshipRepository;
import com.sitms.supervisor.SupervisorRepository;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Business logic for {@link Student} profiles (SITMS-007). */
@Service
public class StudentService {

  public static final String STUDENT_NOT_FOUND = "STUDENT_NOT_FOUND";
  public static final String DUPLICATE_STUDENT = "DUPLICATE_STUDENT";
  public static final String DUPLICATE_STUDENT_NO = "DUPLICATE_STUDENT_NO";
  public static final String DUPLICATE_USERNAME = "DUPLICATE_USERNAME";
  public static final String EMPTY_PHOTO = "EMPTY_PHOTO";
  public static final String INVALID_PHOTO_TYPE = "INVALID_PHOTO_TYPE";
  public static final String PHOTO_TOO_LARGE = "PHOTO_TOO_LARGE";
  public static final String INVALID_PHOTO = "INVALID_PHOTO";

  private static final long MAX_PHOTO_BYTES = 2L * 1024 * 1024;

  private final StudentRepository studentRepository;
  private final SupervisorRepository supervisorRepository;
  private final AcademicStaffRepository academicStaffRepository;
  private final InternshipRepository internshipRepository;
  private final AppUserIdentity appUserIdentity;

  public StudentService(
      StudentRepository studentRepository,
      SupervisorRepository supervisorRepository,
      AcademicStaffRepository academicStaffRepository,
      InternshipRepository internshipRepository,
      AppUserIdentity appUserIdentity) {
    this.studentRepository = studentRepository;
    this.supervisorRepository = supervisorRepository;
    this.academicStaffRepository = academicStaffRepository;
    this.internshipRepository = internshipRepository;
    this.appUserIdentity = appUserIdentity;
  }

  /**
   * Lists students filtered server-side by name/number and/or program, scoped to the principal.
   *
   * <p>ADMIN sees all (or, when {@code supervisorId} is given, only that supervisor's students);
   * SUPERVISOR sees their own assigned students, or those of the supervisor selected via {@code
   * supervisorId}; STUDENT sees only their own profile (self-service access is via {@code /me}).
   */
  public Page<StudentResponse> list(
      PrincipalRole role,
      UUID appUserId,
      UUID supervisorId,
      UUID academicStaffId,
      String q,
      String program,
      String department,
      StudentStatus status,
      Integer batch,
      Pageable pageable) {
    String term = blankToNull(q);
    String prog = blankToNull(program);
    String dept = blankToNull(department);
    Page<Student> page;
    if (role == PrincipalRole.SUPERVISOR) {
      UUID targetSupervisorId =
          supervisorId != null
              ? supervisorId
              : supervisorRepository
                  .findByAppUserId(appUserId)
                  .orElseThrow(
                      () ->
                          new ResourceNotFoundException(
                              "SUPERVISOR_PROFILE_NOT_FOUND",
                              "No supervisor profile for user: " + appUserId))
                  .getId();
      page =
          studentRepository.findAll(
              bySupervisor(targetSupervisorId, term, prog, dept, status, batch), pageable);
    } else if (role == PrincipalRole.FACULTY) {
      if (academicStaffId != null) {
        page =
            studentRepository.findAll(
                byAcademicSupervisor(academicStaffId, term, prog, dept, status, batch), pageable);
      } else {
        UUID ownAcademicStaffId =
            academicStaffRepository
                .findByAppUserId(appUserId)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "ACADEMIC_STAFF_PROFILE_NOT_FOUND",
                            "No academic staff profile for user: " + appUserId))
                .getId();
        page =
            studentRepository.findAll(
                byAcademicSupervisor(ownAcademicStaffId, term, prog, dept, status, batch), pageable);
      }
    } else if (role == PrincipalRole.STUDENT) {
      page = studentRepository.findAll(byOwnProfile(appUserId, term, prog, dept, status, batch), pageable);
    } else if (supervisorId != null) {
      page =
          studentRepository.findAll(
              bySupervisor(supervisorId, term, prog, dept, status, batch), pageable);
    } else {
      page = studentRepository.findAll(search(term, prog, dept, status, batch), pageable);
    }
    return page.map(StudentResponse::fromWithoutPhoto);
  }

  private static Specification<Student> search(
      String q, String program, String department, StudentStatus status, Integer batch) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      appendSearch(cb, predicates, root, q, program, department, status, batch);
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /** The calling student's own profile only. */
  private static Specification<Student> byOwnProfile(
      UUID appUserId, String q, String program, String department, StudentStatus status, Integer batch) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      appendSearch(cb, predicates, root, q, program, department, status, batch);
      predicates.add(cb.equal(root.get("appUserId"), appUserId));
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * The students assigned to the calling supervisor (via internships), filtered by
   * q/program/department/status/batch.
   */
  private static Specification<Student> bySupervisor(
      UUID supervisorId, String q, String program, String department, StudentStatus status, Integer batch) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      appendSearch(cb, predicates, root, q, program, department, status, batch);
      Subquery<UUID> assigned = query.subquery(UUID.class);
      Root<Internship> internship = assigned.from(Internship.class);
      assigned.select(internship.get("studentId"));
      assigned.where(cb.equal(internship.get("supervisorId"), supervisorId));
      predicates.add(root.get("id").in(assigned));
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  /**
   * The students assigned to the calling academic staff member (via internships), filtered by
   * q/program/department/status/batch.
   */
  private static Specification<Student> byAcademicSupervisor(
      UUID academicStaffId, String q, String program, String department, StudentStatus status, Integer batch) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      appendSearch(cb, predicates, root, q, program, department, status, batch);
      Subquery<UUID> assigned = query.subquery(UUID.class);
      Root<Internship> internship = assigned.from(Internship.class);
      assigned.select(internship.get("studentId"));
      assigned.where(cb.equal(internship.get("academicStaffId"), academicStaffId));
      predicates.add(root.get("id").in(assigned));
      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }

  private static void appendSearch(
      CriteriaBuilder cb,
      List<Predicate> predicates,
      Root<Student> root,
      String q,
      String program,
      String department,
      StudentStatus status,
      Integer batch) {
    if (q != null) {
      String like = "%" + q.toLowerCase() + "%";
      Predicate name = cb.like(cb.lower(root.get("fullName")), like);
      Predicate number = cb.like(cb.lower(root.get("studentNo")), like);
      Predicate username = cb.like(cb.lower(root.get("appUserUsername")), like);
      predicates.add(cb.or(name, number, username));
    }
    if (program != null) {
      predicates.add(cb.equal(root.get("program"), program));
    }
    if (department != null) {
      predicates.add(cb.equal(root.get("department"), department));
    }
    if (status != null) {
      predicates.add(cb.equal(root.get("status"), status));
    }
    if (batch != null) {
      predicates.add(cb.equal(root.get("batch"), batch));
    }
  }

  private static String blankToNull(String value) {
    return (value == null || value.isBlank()) ? null : value.trim();
  }

  public StudentResponse get(PrincipalRole role, UUID appUserId, UUID id) {
    Student student = find(id);
    if (role == PrincipalRole.STUDENT && !student.getAppUserId().equals(appUserId)) {
      throw new ResourceNotFoundException(STUDENT_NOT_FOUND, "Student not found: " + id);
    }
    if (role == PrincipalRole.SUPERVISOR
        && !internshipRepository.existsBySupervisorIdAndStudentId(
            supervisorRepository.findByAppUserId(appUserId).orElseThrow().getId(), id)) {
      throw new ResourceNotFoundException(STUDENT_NOT_FOUND, "Student not found: " + id);
    }
    if (role == PrincipalRole.FACULTY
        && !internshipRepository.existsByAcademicStaffIdAndStudentId(
            academicStaffRepository.findByAppUserId(appUserId).orElseThrow().getId(), id)) {
      throw new ResourceNotFoundException(STUDENT_NOT_FOUND, "Student not found: " + id);
    }
    return StudentResponse.from(student);
  }

  /** Returns the calling student's own profile, resolved from their authenticated {@code sub}. */
  public StudentResponse getMine(UUID subject) {
    return StudentResponse.from(findByAppUser(subject));
  }

  @Transactional
  public StudentResponse create(StudentRequest request) {
    String username = request.appUserUsername();
    if (appUserIdentity.existsByUsername(username)) {
      throw new BusinessException(DUPLICATE_USERNAME, "App user id already in use: " + username);
    }
    String email = blankToNull(request.email());
    UUID appUserId =
        appUserIdentity.ensureRole(username, request.fullName(), "STUDENT", email);

    if (studentRepository.existsByStudentNo(request.studentNo())) {
      throw new BusinessException(DUPLICATE_STUDENT_NO, "Student number already exists.");
    }
    if (studentRepository.findByAppUserId(appUserId).isPresent()) {
      throw new BusinessException(DUPLICATE_STUDENT, "A student profile already exists for this user.");
    }

    Student student =
        studentRepository.save(
            Student.builder()
                .appUserId(appUserId)
                .appUserUsername(username)
                .studentNo(request.studentNo())
                .fullName(request.fullName())
                .program(request.program())
                .department(blankToNull(request.department()))
                .university(blankToNull(request.university()))
                .phone(blankToNull(request.phone()))
                .email(email != null ? email : username + "@sitms.local")
                .status(request.statusOrDefault())
                .level(request.levelOrDefault())
                .batch(request.batchOrDefault())
                .build());
    return StudentResponse.from(student);
  }

  @Transactional
  public StudentResponse update(UUID id, StudentRequest request) {
    Student student = find(id);
    return StudentResponse.from(applyUpdate(student, request));
  }

  /** Updates the calling student's own profile; verifies the record belongs to {@code subject}. */
  @Transactional
  public StudentResponse updateMine(UUID subject, StudentProfileUpdateRequest request) {
    Student student = findByAppUser(subject);
    String email = blankToNull(request.email());
    if (email != null && !email.equals(student.getEmail())) {
      appUserIdentity.updateEmail(student.getAppUserId(), email);
    }
    student.updateContact(email, blankToNull(request.phone()));
    return StudentResponse.from(studentRepository.save(student));
  }

  /** Uploads a profile photo for the calling student's own profile. */
  @Transactional
  public StudentResponse uploadMinePhoto(UUID subject, MultipartFile file) {
    return StudentResponse.from(applyPhoto(findByAppUser(subject), file));
  }

  /** Uploads a profile photo for an arbitrary student (admin). */
  @Transactional
  public StudentResponse uploadPhoto(UUID id, MultipartFile file) {
    return StudentResponse.from(applyPhoto(find(id), file));
  }

  private Student applyPhoto(Student student, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw new BusinessException(EMPTY_PHOTO, "Please choose a photo to upload.");
    }
    String contentType = file.getContentType();
    if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
      throw new BusinessException(INVALID_PHOTO_TYPE, "Profile photo must be an image file.");
    }
    if (file.getSize() > MAX_PHOTO_BYTES) {
      throw new BusinessException(PHOTO_TOO_LARGE, "Profile photo must be 2 MB or smaller.");
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw new BusinessException(INVALID_PHOTO, "The uploaded photo could not be read.");
    }
    String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    student.updateProfilePhoto(dataUrl);
    return studentRepository.save(student);
  }

  @Transactional
  public void delete(UUID id) {
    Student student = find(id);
    studentRepository.delete(student);
  }

  private Student find(UUID id) {
    return studentRepository
        .findById(id)
        .orElseThrow(
            () -> new ResourceNotFoundException(STUDENT_NOT_FOUND, "Student not found: " + id));
  }

  private Student findByAppUser(UUID appUserId) {
    return studentRepository
        .findByAppUserId(appUserId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    STUDENT_NOT_FOUND, "No student profile for user: " + appUserId));
  }

  private Student applyUpdate(Student student, StudentRequest request) {
    String username = request.appUserUsername();
    if (username != null && !username.equals(student.getAppUserUsername())) {
      if (appUserIdentity.existsByUsername(username)) {
        throw new BusinessException(DUPLICATE_USERNAME, "App user id already in use: " + username);
      }
      appUserIdentity.rename(student.getAppUserId(), username);
      student.renameTo(username);
    }
    if (!student.getStudentNo().equals(request.studentNo())
        && studentRepository.existsByStudentNo(request.studentNo())) {
      throw new BusinessException(DUPLICATE_STUDENT_NO, "Student number already exists.");
    }
    String email = blankToNull(request.email());
    String resolvedEmail = email != null ? email : student.getEmail();
    if (resolvedEmail != null && !resolvedEmail.equals(student.getEmail())) {
      appUserIdentity.updateEmail(student.getAppUserId(), resolvedEmail);
    }
    student.updateProfile(
        request.studentNo(),
        request.fullName(),
        request.program(),
        blankToNull(request.department()),
        blankToNull(request.university()),
        request.statusOrDefault(),
        request.levelOrDefault(),
        request.batch() != null ? request.batch() : student.getBatch(),
        blankToNull(request.phone()),
        resolvedEmail);
    return studentRepository.save(student);
  }
}
