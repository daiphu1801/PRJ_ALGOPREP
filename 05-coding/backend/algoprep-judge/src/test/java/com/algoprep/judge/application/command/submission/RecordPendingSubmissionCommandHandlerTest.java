package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.application.dto.RecordPendingSubmissionCommand;
import com.algoprep.judge.domain.exception.ProblemGradingReadException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionException;
import com.algoprep.judge.domain.exception.SubmissionAdmissionUnavailableException;
import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class RecordPendingSubmissionCommandHandlerTest {
  @Test
  void admissionPinsTheTestcaseVersionAndKeepsSourceByteForByte() {
    var f = new SubmissionAdmissionFixture();
    var ticket = f.handler().recordPending(f.command());
    assertThat(ticket.submissionId()).isEqualTo(f.submissionId);
    assertThat(ticket.status()).isEqualTo("PENDING");
    assertThat(ticket.submittedAt()).isEqualTo(f.at);
    assertThat(ticket.totalCountHint()).isEqualTo(1);
    assertThat(ticket.programType().language()).isEqualTo("PYTHON");
    var sequence = inOrder(f.repository);
    sequence.verify(f.repository).languageEnabled("PYTHON");
    sequence.verify(f.repository).lockPair(f.owner);
    sequence.verify(f.repository).databaseNow();
    sequence.verify(f.repository).countRecent(f.owner, f.at);
    var capture = ArgumentCaptor.forClass(PendingSubmission.class);
    sequence.verify(f.repository).insert(capture.capture());
    assertThat(capture.getValue().testcaseSetVersion()).isEqualTo(4);
    assertThat(capture.getValue().program()).isEqualTo(f.command().program());
    assertThat(capture.getValue().toString()).doesNotContain("đáp án");
    assertThat(f.command().toString()).doesNotContain("đáp án");
  }

  @ParameterizedTest
  @CsvSource({
    "true,true,true,1,OK",
    "false,true,true,1,JDG-101",
    "true,true,true,0,JDG-104",
    "true,false,true,1,JDG-504",
    "true,true,false,1,JDG-103"
  })
  void problemGuardsRejectBeforeLockOrWrite(
      boolean published, boolean ready, boolean wrapper, int count, String code) {
    var f = new SubmissionAdmissionFixture();
    f.spec(published, ready, wrapper, count, null);
    var cmd = f.command("pass", "PYTHON", "FUNCTION_WRAPPER", "EDITOR", null);
    if (code.equals("OK")) {
      assertThat(f.handler().recordPending(cmd).status()).isEqualTo("PENDING");
    } else {
      rejected(() -> f.handler().recordPending(cmd), code);
      verify(f.repository, never()).lockPair(any());
      verify(f.repository, never()).insert(any());
    }
  }

  @Test
  void missingProblemAndDisabledLanguageFailClosed() {
    var f = new SubmissionAdmissionFixture();
    when(f.problems.loadGradingSpec(f.owner.problemId())).thenReturn(Optional.empty());
    rejected(() -> f.handler().recordPending(f.command()), "JDG-101");
    f.spec(true, true, true, 1, null);
    when(f.repository.languageEnabled("PYTHON")).thenReturn(false);
    rejected(() -> f.handler().recordPending(f.command()), "JDG-102");
    verify(f.repository, never()).insert(any());
  }

  @ParameterizedTest
  @CsvSource({"60,59,true", "60,60,false", "5,4,true", "5,5,false", "5,6,false"})
  void quotasUseGreaterThanOrEqualAndReturnOnlyPublicDetails(
      int limit, long count, boolean accepted) {
    var f = new SubmissionAdmissionFixture();
    f.spec(true, true, true, 1, limit == 60 ? null : limit);
    when(f.repository.countRecent(f.owner, f.at)).thenReturn(count);
    if (accepted) {
      f.handler().recordPending(f.command());
      verify(f.repository).insert(any());
    } else {
      assertThatThrownBy(() -> f.handler().recordPending(f.command()))
          .isInstanceOfSatisfying(
              SubmissionAdmissionException.class,
              e -> {
                assertThat(e.errorCode()).isEqualTo("JDG-106");
                assertThat(e.clientErrorDetails())
                    .containsEntry("limit", limit)
                    .containsEntry("windowMinutes", 60);
              });
      verify(f.repository, never()).insert(any());
      verifyNoInteractions(f.ids);
    }
  }

  @Test
  void defaultLimitCanBeConfiguredAndInvalidSettingsDoNotSilentlyFallback() {
    var f = new SubmissionAdmissionFixture();
    when(f.repository.countRecent(f.owner, f.at)).thenReturn(7L);
    var handler =
        new RecordPendingSubmissionCommandHandler(f.repository, f.problems, f.ids, f.manager, 7);
    rejected(() -> handler.recordPending(f.command()), "JDG-106");
    var invalid =
        new RecordPendingSubmissionCommandHandler(f.repository, f.problems, f.ids, f.manager, 0);
    assertThatThrownBy(() -> invalid.recordPending(f.command()))
        .isInstanceOf(SubmissionAdmissionUnavailableException.class);
    f.spec(true, true, true, 1, 0);
    assertThatThrownBy(() -> f.handler().recordPending(f.command()))
        .isInstanceOf(SubmissionAdmissionUnavailableException.class);
    verify(f.repository, never()).insert(any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"spec", "enabled", "lock", "clock", "count", "insert", "id"})
  void infrastructureErrorsNeverExposeSourceOrSql(String phase) {
    var f = new SubmissionAdmissionFixture();
    var raw = new IllegalStateException("SQL secret-source");
    switch (phase) {
      case "spec" -> when(f.problems.loadGradingSpec(any())).thenThrow(raw);
      case "enabled" -> when(f.repository.languageEnabled(any())).thenThrow(raw);
      case "lock" -> doThrow(raw).when(f.repository).lockPair(any());
      case "clock" -> when(f.repository.databaseNow()).thenThrow(raw);
      case "count" -> when(f.repository.countRecent(any(), any())).thenThrow(raw);
      case "id" -> when(f.ids.newId()).thenReturn(null);
      default -> doThrow(raw).when(f.repository).insert(any());
    }
    assertThatThrownBy(() -> f.handler().recordPending(f.command()))
        .isInstanceOf(SubmissionAdmissionUnavailableException.class)
        .hasMessage("Submission admission is unavailable")
        .hasNoCause();
    if (!phase.equals("insert")) {
      verify(f.repository, never()).insert(any());
    }
  }

  @Test
  void typedGradingReadFailuresKeepTheirClassification() {
    var f = new SubmissionAdmissionFixture();
    var error =
        new ProblemGradingReadException(ProblemGradingReadException.Reason.SPEC_UNAVAILABLE);
    when(f.problems.loadGradingSpec(any())).thenThrow(error);
    assertThatThrownBy(() -> f.handler().recordPending(f.command())).isSameAs(error);
    verifyNoInteractions(f.repository);
  }

  @Test
  void nullProblemIsNotFoundAndNullCallerCannotWrite() {
    var f = new SubmissionAdmissionFixture();
    rejected(
        () ->
            f.handler()
                .recordPending(
                    new RecordPendingSubmissionCommand(
                        new Owner(f.owner.userId(), null), f.command().program(), null)),
        "JDG-101");
    assertThatThrownBy(
            () ->
                f.handler()
                    .recordPending(
                        new RecordPendingSubmissionCommand(
                            new Owner(null, f.owner.problemId()), f.command().program(), null)))
        .isInstanceOf(NullPointerException.class);
    verifyNoInteractions(f.repository, f.problems);
  }

  static void rejected(Runnable action, String code) {
    assertThatThrownBy(action::run)
        .isInstanceOfSatisfying(
            SubmissionAdmissionException.class, e -> assertThat(e.errorCode()).isEqualTo(code))
        .hasNoCause();
  }
}
