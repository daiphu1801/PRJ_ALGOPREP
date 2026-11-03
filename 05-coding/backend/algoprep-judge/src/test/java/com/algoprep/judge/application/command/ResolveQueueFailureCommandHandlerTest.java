package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.algoprep.judge.domain.exception.QueueFailureFinalizationUnavailableException;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

class ResolveQueueFailureCommandHandlerTest {
  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void finalizationWritesTheExactGradedEnvelopeWithNullScores(boolean firstAttempt) {
    var f = new QueueFailureFixture();
    when(f.repository.firstAttempt(f.view)).thenReturn(firstAttempt);
    assertThat(f.handler().resolve(f.id).finalized()).isTrue();
    var body = ArgumentCaptor.forClass(String.class);
    verify(f.repository)
        .appendGraded(
            org.mockito.ArgumentMatchers.eq(f.eventId),
            org.mockito.ArgumentMatchers.eq(f.id),
            body.capture(),
            org.mockito.ArgumentMatchers.eq(f.finished));
    var event = JsonMapper.builder().build().readTree(body.getValue());
    assertThat(event.size()).isEqualTo(5);
    assertThat(event.path("eventId").asText()).isEqualTo(f.eventId.toString());
    assertThat(event.path("eventType").asText()).isEqualTo("SubmissionGraded");
    assertThat(event.path("schemaVersion").asInt()).isEqualTo(1);
    assertThat(event.path("occurredAt").asText()).isEqualTo(f.finished.toString());
    var payload = event.path("payload");
    assertThat(payload.size()).isEqualTo(10);
    assertThat(payload.path("submissionId").asText()).isEqualTo(f.id.toString());
    assertThat(payload.path("userId").asText()).isEqualTo(f.owner.userId().toString());
    assertThat(payload.path("problemId").asText()).isEqualTo(f.owner.problemId().toString());
    assertThat(payload.path("language").asText()).isEqualTo("PYTHON");
    assertThat(payload.path("verdict").asText()).isEqualTo("SYSTEM_ERROR");
    assertThat(payload.path("firstAttemptForProblem").asBoolean()).isEqualTo(firstAttempt);
    assertThat(payload.path("submittedAt").asText()).isEqualTo(f.submitted.toString());
    for (String field : new String[] {"passedCount", "totalCount", "passedTestcaseRatio"}) {
      assertThat(payload.path(field).isNull()).isTrue();
    }
    assertThat(body.getValue())
        .doesNotContain("sourceCode", "failureReason", "input", "stderr", "SubmissionAccepted");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "COMPILING",
        "JUDGING",
        "ACCEPTED",
        "WRONG_ANSWER",
        "TIME_LIMIT_EXCEEDED",
        "RUNTIME_ERROR",
        "COMPILE_ERROR",
        "SYSTEM_ERROR"
      })
  void aWorkerOrTerminalWinnerIsReadWithoutAnotherEvent(String status) {
    var f = new QueueFailureFixture();
    when(f.repository.finishPending(f.id)).thenReturn(Optional.empty());
    var view = new SubmissionLifecycleView(f.id, f.owner, "PYTHON", status, f.view.timing());
    when(f.repository.find(f.id)).thenReturn(Optional.of(view));
    var result = f.handler().resolve(f.id);
    assertThat(result.finalized()).isFalse();
    assertThat(result.submission().status()).isEqualTo(status);
    verifyNoInteractions(f.ids);
    verify(f.repository, never()).appendGraded(any(), any(), any(), any());
  }

  @ParameterizedTest
  @ValueSource(strings = {"update", "read", "first", "append", "id", "commit"})
  void everyInfrastructureFailureIncludingCommitIsSanitized(String phase) {
    var f = new QueueFailureFixture();
    var raw = new IllegalStateException("SQL secret-source");
    switch (phase) {
      case "update" -> when(f.repository.finishPending(f.id)).thenThrow(raw);
      case "read" -> {
        when(f.repository.finishPending(f.id)).thenReturn(Optional.empty());
        when(f.repository.find(f.id)).thenThrow(raw);
      }
      case "first" -> when(f.repository.firstAttempt(f.view)).thenThrow(raw);
      case "append" -> doThrow(raw).when(f.repository).appendGraded(any(), any(), any(), any());
      case "id" -> when(f.ids.newId()).thenReturn(null);
      default -> doThrow(raw).when(f.manager).commit(any());
    }
    assertThatThrownBy(() -> f.handler().resolve(f.id))
        .isInstanceOf(QueueFailureFinalizationUnavailableException.class)
        .hasMessage("Submission queue failure could not be resolved")
        .hasNoCause();
  }

  @Test
  void missingSubmissionDoesNotBecomeAFakeTerminalResult() {
    var f = new QueueFailureFixture();
    when(f.repository.finishPending(f.id)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> f.handler().resolve(f.id))
        .isInstanceOf(QueueFailureFinalizationUnavailableException.class);
    verifyNoInteractions(f.ids);
    assertThatThrownBy(() -> f.handler().resolve(null)).isInstanceOf(NullPointerException.class);
  }
}
