package com.algoprep.interviewbank.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import com.algoprep.common.exception.DomainRuleViolationException;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.common.exception.ResourceConflictException;
import com.algoprep.common.exception.ResourceNotFoundException;
import com.algoprep.interviewbank.application.dto.PracticeSubmission;
import com.algoprep.interviewbank.domain.exception.AnswerTooLongException;
import com.algoprep.interviewbank.domain.exception.AttemptNumberConflictException;
import com.algoprep.interviewbank.domain.exception.FeedbackInvalidException;
import com.algoprep.interviewbank.domain.exception.FeedbackUnavailableException;
import com.algoprep.interviewbank.domain.exception.PracticeBudgetLockedException;
import com.algoprep.interviewbank.domain.exception.PracticeQuotaExceededException;
import com.algoprep.interviewbank.domain.model.AnswerAttempt;
import com.algoprep.interviewbank.domain.model.FeedbackRequest;
import com.algoprep.interviewbank.domain.model.FeedbackResult;
import com.algoprep.interviewbank.domain.model.FeedbackStatus;
import com.algoprep.interviewbank.domain.model.GateTicket;
import com.algoprep.interviewbank.domain.model.LearnerCategory;
import com.algoprep.interviewbank.domain.model.NewAttempt;
import com.algoprep.interviewbank.domain.model.PracticePolicy;
import com.algoprep.interviewbank.domain.model.PracticeQuestion;
import com.algoprep.interviewbank.domain.model.RubricCriterion;
import com.algoprep.interviewbank.domain.ports.out.AnswerFeedbackPort;
import com.algoprep.interviewbank.domain.ports.out.PracticeRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubmitPracticeAnswerCommandHandlerTest {
  private final PracticeRepository repository = mock(PracticeRepository.class);
  private final PracticeAttemptStore store = mock(PracticeAttemptStore.class);
  private final AnswerFeedbackPort ai = mock(AnswerFeedbackPort.class);
  private final IdGeneratorPort ids = mock(IdGeneratorPort.class);
  private final SubmitPracticeAnswerCommandHandler handler =
      new SubmitPracticeAnswerCommandHandler(repository, store, ai, ids);
  private final UUID user = UUID.randomUUID();
  private final UUID questionId = UUID.randomUUID();
  private final UUID answerId = UUID.randomUUID();
  private final UUID callId = UUID.randomUUID();
  private final GateTicket ticket = new GateTicket(user, "req", 7, true);
  private final List<RubricCriterion> rubric =
      List.of(
          new RubricCriterion("Clarity", new BigDecimal("60.00"), null),
          new RubricCriterion("Depth", new BigDecimal("40.00"), "why"));

  private PracticeSubmission submission(String text) {
    return new PracticeSubmission(user, LearnerCategory.STUDENT, questionId, text);
  }

  private void question(List<RubricCriterion> criteria) {
    when(repository.findActiveQuestion(questionId))
        .thenReturn(Optional.of(new PracticeQuestion(questionId, "T", "C", criteria)));
    when(ids.newId()).thenReturn(answerId, callId);
  }

  private static FeedbackResult result() {
    return new FeedbackResult(
        List.of("a"),
        List.of(),
        List.of("s"),
        List.of(
            new FeedbackResult.CriterionScore("Clarity", 70),
            new FeedbackResult.CriterionScore("Depth", 20)));
  }

  private static AnswerAttempt echo(NewAttempt n, int no) {
    return new AnswerAttempt(n.id(), no, Instant.EPOCH, n.answerText(), n.status(), n.feedback());
  }

  private void stored() {
    when(store.save(any())).thenAnswer(call -> echo(call.getArgument(0), 1));
  }

  @Test
  void goodFeedbackIsStoredCompletedWithTheSnapshotWeights() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenReturn(result());
    stored();

    var outcome = handler.submit(submission("my answer"));

    assertThat(outcome.feedbackErrorCode()).isNull();
    assertThat(outcome.attempt().status()).isEqualTo(FeedbackStatus.COMPLETED);
    assertThat(outcome.attempt().feedback().criteria())
        .extracting(c -> c.weightPercent().toPlainString())
        .containsExactly("60.00", "40.00");
    var request = ArgumentCaptor.forClass(FeedbackRequest.class);
    verify(ai).compareAnswer(request.capture());
    assertThat(request.getValue().systemInstruction()).isEqualTo(PracticePolicy.SYSTEM_INSTRUCTION);
    assertThat(request.getValue().systemInstruction()).doesNotContain("my answer");
    assertThat(request.getValue().answerText()).isEqualTo("my answer");
    assertThat(request.getValue().callId()).isEqualTo(callId);
    assertThat(request.getValue().answerId()).isEqualTo(answerId);
    verify(ai, never()).refund(any());
  }

  @Test
  void theAnswerIsStoredAsTypedWithoutTrimming() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenReturn(result());
    stored();

    var outcome = handler.submit(submission("  keep  me </answer>\n"));

    assertThat(outcome.attempt().answerText()).isEqualTo("  keep  me </answer>\n");
  }

  @Test
  void inputIsCheckedBeforeAnyReadOrGate() {
    assertThatThrownBy(() -> handler.submit(submission("   ")))
        .isInstanceOfSatisfying(
            InvalidRequestException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-V06"));
    assertThatThrownBy(() -> handler.submit(submission(null)))
        .isInstanceOf(InvalidRequestException.class);
    assertThatThrownBy(() -> handler.submit(submission("x".repeat(5001))))
        .isInstanceOfSatisfying(
            AnswerTooLongException.class,
            e -> assertThat(e.clientErrorDetails()).containsEntry("maxLength", 5000));
    verify(repository, never()).findActiveQuestion(any());
    verify(ai, never()).reserve(any(), any(), anyInt());
  }

  @Test
  void exactlyFiveThousandCharactersAreAccepted() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenReturn(result());
    stored();

    assertThat(handler.submit(submission("x".repeat(5000))).attempt().attemptNo()).isEqualTo(1);
  }

  @Test
  void aMissingOrRetiredQuestionIsInb010() {
    when(repository.findActiveQuestion(questionId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> handler.submit(submission("a")))
        .isInstanceOfSatisfying(
            ResourceNotFoundException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-010"));
  }

  @Test
  void aQuestionWithoutRubricIsInb040AndTouchesNoGate() {
    question(List.of());

    assertThatThrownBy(() -> handler.submit(submission("a")))
        .isInstanceOfSatisfying(
            DomainRuleViolationException.class,
            e -> assertThat(e.errorCode()).isEqualTo("INB-040"));
    verify(ai, never()).reserve(any(), any(), anyInt());
  }

  @Test
  void aRefusedQuotaOrBudgetStoresNothingAndCallsNoModel() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20))
        .thenThrow(new PracticeQuotaExceededException(30))
        .thenThrow(new PracticeBudgetLockedException());

    assertThatThrownBy(() -> handler.submit(submission("a")))
        .isInstanceOf(PracticeQuotaExceededException.class);
    assertThatThrownBy(() -> handler.submit(submission("a")))
        .isInstanceOf(PracticeBudgetLockedException.class);
    verify(ai, never()).compareAnswer(any());
    verify(store, never()).save(any());
  }

  @Test
  void aBrokenGateStoresAFailedAttemptWithInb501AndNeverCallsTheModel() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20))
        .thenThrow(new FeedbackUnavailableException(null));
    stored();

    var outcome = handler.submit(submission("a"));

    assertThat(outcome.feedbackErrorCode()).isEqualTo("INB-501");
    assertThat(outcome.attempt().status()).isEqualTo(FeedbackStatus.FAILED);
    assertThat(outcome.attempt().feedback()).isNull();
    verify(ai, never()).compareAnswer(any());
    verify(ai, never()).refund(any());
  }

  @Test
  void aTimeoutStoresFailedInb501AndRefundsTheSlotOnce() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenThrow(new FeedbackUnavailableException(null));
    stored();

    var outcome = handler.submit(submission("a"));

    assertThat(outcome.feedbackErrorCode()).isEqualTo("INB-501");
    assertThat(outcome.attempt().status()).isEqualTo(FeedbackStatus.FAILED);
    verify(ai, times(1)).refund(ticket);
  }

  @Test
  void anUnexpectedBridgeErrorDegradesTheSameWayAsATimeout() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenThrow(new IllegalStateException("bridge"));
    stored();

    assertThat(handler.submit(submission("a")).feedbackErrorCode()).isEqualTo("INB-501");
    verify(ai).refund(ticket);
  }

  @Test
  void aBadResultStoresFailedInb502AndKeepsTheSlotUsed() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenThrow(new FeedbackInvalidException(null));
    stored();

    var outcome = handler.submit(submission("a"));

    assertThat(outcome.feedbackErrorCode()).isEqualTo("INB-502");
    assertThat(outcome.attempt().status()).isEqualTo(FeedbackStatus.FAILED);
    verify(ai, never()).refund(any());
  }

  @Test
  void aCodeOutsideTheSnapshotIsTreatedAsABadResult() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any()))
        .thenReturn(
            new FeedbackResult(
                List.of(),
                List.of(),
                List.of(),
                List.of(new FeedbackResult.CriterionScore("Ghost", 1))));
    stored();

    assertThat(handler.submit(submission("a")).feedbackErrorCode()).isEqualTo("INB-502");
  }

  @Test
  void aParallelNumberClashIsRetriedWithTheSameAnswerIdAndAiResult() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenReturn(result());
    when(store.save(any()))
        .thenThrow(new AttemptNumberConflictException(null))
        .thenAnswer(call -> echo(call.getArgument(0), 2));

    var outcome = handler.submit(submission("a"));

    assertThat(outcome.attempt().attemptNo()).isEqualTo(2);
    verify(ai, times(1)).compareAnswer(any());
    verify(store, times(2)).save(any());
  }

  @Test
  void threeClashesInARowAreInb050() {
    question(rubric);
    when(ai.reserve(user, LearnerCategory.STUDENT, 20)).thenReturn(ticket);
    when(ai.compareAnswer(any())).thenReturn(result());
    when(store.save(any())).thenThrow(new AttemptNumberConflictException(null));

    assertThatThrownBy(() -> handler.submit(submission("a")))
        .isInstanceOfSatisfying(
            ResourceConflictException.class, e -> assertThat(e.errorCode()).isEqualTo("INB-050"));
    verify(store, times(3)).save(any());
    verify(ai, times(1)).compareAnswer(any());
  }
}
