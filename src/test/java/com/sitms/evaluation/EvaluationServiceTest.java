package com.sitms.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sitms.audit.AuditAction;
import com.sitms.audit.AuditLogService;
import com.sitms.common.exception.BusinessException;
import com.sitms.internship.InternshipRepository;
import com.sitms.notification.NotificationNotifier;
import com.sitms.notification.NotificationType;
import com.sitms.student.Student;
import com.sitms.student.StudentRepository;
import com.sitms.supervisor.Supervisor;
import com.sitms.supervisor.SupervisorRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link EvaluationService} (SITMS-058): the role-scoping rules (supervisors only
 * their own/assigned students, students only their own records, admins designate the supervisor),
 * the submit-immutability rule, and the notification/audit wiring on {code SUBMITTED} creation.
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

  private static final UUID APP_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final UUID SUPERVISOR_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private static final UUID OTHER_SUPERVISOR_ID =
      UUID.fromString("33333333-3333-3333-3333-333333333333");
  private static final UUID STUDENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
  private static final UUID OTHER_STUDENT_ID =
      UUID.fromString("55555555-5555-5555-5555-555555555555");
  private static final UUID STUDENT_APP_USER_ID =
      UUID.fromString("66666666-6666-6666-6666-666666666666");
  private static final UUID INTERNSHIP_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
  private static final UUID EVALUATION_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");
  private static final Pageable PAGEABLE = PageRequest.of(0, 20);

  @Mock private EvaluationRepository evaluationRepository;
  @Mock private StudentRepository studentRepository;
  @Mock private SupervisorRepository supervisorRepository;
  @Mock private InternshipRepository internshipRepository;
  @Mock private NotificationNotifier notifier;
  @Mock private AuditLogService auditLogService;
  @InjectMocks private EvaluationService evaluationService;

  @Test
  void supervisorCreatesSubmittedEvaluationForAssignedStudentAndWiresEvents() {
    Supervisor supervisor = supervisor(SUPERVISOR_ID);
    Student recipient = studentRecipient(STUDENT_APP_USER_ID);
    when(supervisorRepository.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(supervisor));
    when(internshipRepository.existsBySupervisorIdAndStudentId(SUPERVISOR_ID, STUDENT_ID))
        .thenReturn(true);
    when(studentRepository.findById(STUDENT_ID)).thenReturn(Optional.of(recipient));
    when(evaluationRepository.saveAndFlush(any(Evaluation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EvaluationRequest request =
        new EvaluationRequest(
            STUDENT_ID, 5, 4, 4, 5, 4, "strengths", "improvement", "comments",
            INTERNSHIP_ID, null, null,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);

    EvaluationResponse response = evaluationService.create(request, "SUPERVISOR", APP_USER_ID);

    ArgumentCaptor<Evaluation> captured = ArgumentCaptor.forClass(Evaluation.class);
    verify(evaluationRepository).saveAndFlush(captured.capture());
    Evaluation saved = captured.getValue();
    assertThat(saved.getStudentId()).isEqualTo(STUDENT_ID);
    assertThat(saved.getInternshipId()).isEqualTo(INTERNSHIP_ID);
    assertThat(saved.getSupervisorId()).isEqualTo(SUPERVISOR_ID);
    assertThat(saved.getStatus()).isEqualTo(EvaluationStatus.SUBMITTED);
    assertThat(saved.getProfessionalism()).isEqualTo(4);
    assertThat(saved.getOverallScore())
        .isEqualTo(
            Evaluation.computeOverallScore(
                5, 4, 4, 5, 4)); /* mean 4.4 scaled → 88 */
    assertThat(response).isNotNull();
    verify(notifier)
        .notify(
            STUDENT_APP_USER_ID,
            NotificationType.EVALUATION_SUBMITTED,
            "New evaluation available",
            "A supervisor has submitted an evaluation for your internship.",
            "/evaluations/mine");
    verify(auditLogService)
        .record(
            AuditAction.EVALUATION_SUBMITTED,
            "EVALUATION",
            saved.getId(),
            "Evaluation submitted for student " + STUDENT_ID + ".");
  }

  @Test
  void supervisorCannotEvaluateUnassignedStudent() {
    Supervisor supervisor = supervisor(SUPERVISOR_ID);
    when(supervisorRepository.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(supervisor));
    when(internshipRepository.existsBySupervisorIdAndStudentId(SUPERVISOR_ID, STUDENT_ID))
        .thenReturn(false);

    EvaluationRequest request = requestFor(STUDENT_ID);

    assertThatThrownBy(() -> evaluationService.create(request, "SUPERVISOR", APP_USER_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining(STUDENT_ID.toString());
    verify(evaluationRepository, never()).saveAndFlush(any());
    verify(notifier, never()).notify(any(), any(), any(), any(), any());
  }

  @Test
  void adminMustNameTheReviewingSupervisor() {
    EvaluationRequest request = requestFor(STUDENT_ID);

    assertThatThrownBy(() -> evaluationService.create(request, "ADMIN", APP_USER_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("supervisorId");
    verify(evaluationRepository, never()).saveAndFlush(any());
  }

  @Test
  void submittedEvaluationCannotBeRevertedToDraft() {
    Evaluation existing =
        evaluation(EvaluationStatus.SUBMITTED, STUDENT_ID, SUPERVISOR_ID);
    when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(existing));
    EvaluationRequest request =
        new EvaluationRequest(
            STUDENT_ID, 3, 3, 3, 3, 3, null, null, null, null, null, EvaluationStatus.DRAFT,
            null, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null);

    assertThatThrownBy(() -> evaluationService.update(EVALUATION_ID, request, "ADMIN", APP_USER_ID))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("cannot be reverted");
    verify(evaluationRepository, never()).saveAndFlush(any());
  }

  @Test
  void supervisorCannotUpdateEvaluationOwnedByAnotherSupervisor() {
    Evaluation existing = evaluation(EvaluationStatus.DRAFT, STUDENT_ID, OTHER_SUPERVISOR_ID);
    Supervisor supervisor = supervisor(SUPERVISOR_ID);
    when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(existing));
    when(supervisorRepository.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(supervisor));

    EvaluationRequest request = requestFor(STUDENT_ID);

    assertThatThrownBy(() -> evaluationService.update(EVALUATION_ID, request, "SUPERVISOR", APP_USER_ID))
        .isInstanceOf(AccessDeniedException.class)
        .hasMessageContaining(EVALUATION_ID.toString());
    verify(evaluationRepository, never()).saveAndFlush(any());
  }

  @Test
  void studentCannotReadAnotherStudentsEvaluation() {
    Evaluation existing = evaluation(EvaluationStatus.SUBMITTED, OTHER_STUDENT_ID, SUPERVISOR_ID);
    Student profile = student(STUDENT_ID);
    when(evaluationRepository.findById(EVALUATION_ID)).thenReturn(Optional.of(existing));
    when(studentRepository.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(profile));

    assertThatThrownBy(() -> evaluationService.get(EVALUATION_ID, "STUDENT", APP_USER_ID))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void studentListIsScopedToOwnSubmittedEvaluations() {
    Student profile = student(STUDENT_ID);
    when(studentRepository.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(profile));
    when(evaluationRepository.findByStudentIdAndStatus(
            STUDENT_ID, EvaluationStatus.SUBMITTED, PAGEABLE))
        .thenReturn(Page.empty());

    Page<EvaluationResponse> page = evaluationService.list("STUDENT", APP_USER_ID, PAGEABLE);

    verify(evaluationRepository)
        .findByStudentIdAndStatus(STUDENT_ID, EvaluationStatus.SUBMITTED, PAGEABLE);
    assertThat(page).isEmpty();
  }

  @Test
  void supervisorCreatesEvaluationWithPaperCriteriaAndComputesPaperScore() {
    Supervisor supervisor = supervisor(SUPERVISOR_ID);
    when(supervisorRepository.findByAppUserId(APP_USER_ID)).thenReturn(Optional.of(supervisor));
    when(internshipRepository.existsBySupervisorIdAndStudentId(SUPERVISOR_ID, STUDENT_ID))
        .thenReturn(true);
    when(evaluationRepository.saveAndFlush(any(Evaluation.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    EvaluationRequest request =
        new EvaluationRequest(
            STUDENT_ID, 5, 4, 4, 5, 4, null, null, null, INTERNSHIP_ID, null, null,
            5, 5, 5, 5, 5,
            5, 5, 5, 5, 5,
            5, 5, 5, 15, 20,
            null, "CS", "Acme Corp");

    evaluationService.create(request, "SUPERVISOR", APP_USER_ID);

    ArgumentCaptor<Evaluation> captured = ArgumentCaptor.forClass(Evaluation.class);
    verify(evaluationRepository).saveAndFlush(captured.capture());
    Evaluation saved = captured.getValue();
    assertThat(saved.getDepartment()).isEqualTo("CS");
    assertThat(saved.getCompanyName()).isEqualTo("Acme Corp");
    assertThat(saved.getPunctuality()).isEqualTo(5);
    assertThat(saved.getTaskResponsibility()).isEqualTo(15);
    assertThat(saved.getQualityTeamMember()).isEqualTo(20);
    assertThat(saved.getOverallScore()).isEqualTo(100);
  }

  private static EvaluationRequest requestFor(UUID studentId) {
    return new EvaluationRequest(
        studentId, 4, 4, 4, 4, 4, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null);
  }

  private static Evaluation evaluation(EvaluationStatus status, UUID studentId, UUID supervisorId) {
    return Evaluation.builder()
        .studentId(studentId)
        .supervisorId(supervisorId)
        .technicalSkills(4)
        .communication(4)
        .teamwork(4)
        .problemSolving(4)
        .professionalism(4)
        .status(status)
        .build();
  }

  private static Supervisor supervisor(UUID id) {
    Supervisor supervisor = mock(Supervisor.class);
    when(supervisor.getId()).thenReturn(id);
    return supervisor;
  }

  private static Student student(UUID id) {
    Student student = mock(Student.class);
    when(student.getId()).thenReturn(id);
    return student;
  }

  private static Student studentRecipient(UUID appUserId) {
    Student student = mock(Student.class);
    when(student.getAppUserId()).thenReturn(appUserId);
    return student;
  }
}