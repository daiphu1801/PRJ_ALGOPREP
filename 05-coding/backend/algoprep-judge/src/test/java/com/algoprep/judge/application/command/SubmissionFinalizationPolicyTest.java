package com.algoprep.judge.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.Result;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class SubmissionFinalizationPolicyTest {
  static State state(String status, Integer total) {
    return new State(
        UUID.randomUUID(),
        new Owner(UUID.randomUUID(), UUID.randomUUID()),
        "PYTHON",
        "STANDARD_IO",
        status,
        total,
        0,
        null,
        null,
        Instant.EPOCH,
        Instant.EPOCH.plusSeconds(5));
  }

  @ParameterizedTest
  @CsvSource({
    "AC,ACCEPTED,1.000",
    "WA,WRONG_ANSWER,0.333",
    "TLE,TIME_LIMIT_EXCEEDED,0.333",
    "MLE,TIME_LIMIT_EXCEEDED,0.333",
    "RE,RUNTIME_ERROR,0.333"
  })
  void firstFailureDeterminesStatusWhileAllRowsDetermineScore(
      String verdict, String status, String ratio) {
    var s = state("JUDGING", 3);
    var result =
        SubmissionFinalizationPolicy.summarize(
            s,
            new FinishCommand(s.id(), FinishKind.COMPLETE, null, null),
            List.of(
                new Result(1, "AC", 2, 9),
                new Result(2, verdict, 7, 2),
                new Result(3, verdict, 3, 12)));
    assertThat(result.status()).isEqualTo(status);
    assertThat(result.score()).isEqualByComparingTo(ratio);
    assertThat(result.runtime()).isEqualTo(7);
    assertThat(result.memory()).isEqualTo(12);
    assertThat(result.firstFailingOrder()).isEqualTo(verdict.equals("AC") ? null : 2);
  }

  @Test
  void halfUpRoundingAndEmptyMetricsAreExplicit() {
    var s = state("JUDGING", 16);
    var rows =
        java.util.stream.IntStream.rangeClosed(1, 16)
            .mapToObj(i -> new Result(i, i == 1 ? "AC" : "WA", null, null))
            .toList();
    var result =
        SubmissionFinalizationPolicy.summarize(
            s, new FinishCommand(s.id(), FinishKind.COMPLETE, null, null), rows);
    assertThat(result.score()).isEqualByComparingTo("0.063");
    assertThat(result.runtime()).isNull();
    assertThat(result.memory()).isNull();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"SANDBOX_FAULT", "SPEC_UNAVAILABLE", "WORKER_TIMEOUT", "QUEUE_UNAVAILABLE"})
  void infrastructureFailureKeepsCountersButNeverCreatesAScore(String reason) {
    var s = reason.equals("QUEUE_UNAVAILABLE") ? state("PENDING", null) : state("JUDGING", 3);
    var result =
        SubmissionFinalizationPolicy.summarize(
            s, new FinishCommand(s.id(), FinishKind.SYSTEM_ERROR, reason, null), List.of());
    assertThat(result.status()).isEqualTo("SYSTEM_ERROR");
    assertThat(result.total()).isEqualTo(s.total());
    assertThat(result.score()).isNull();
    assertThat(result.failureReason()).isEqualTo(reason);
    assertThat(result.toString()).doesNotContain(reason);
  }

  @Test
  void compileErrorHasNoCountsOrScoreAndCapsMappedText() {
    var s = state("COMPILING", null);
    var command = new FinishCommand(s.id(), FinishKind.COMPILE_ERROR, null, "private".repeat(2000));
    var result = SubmissionFinalizationPolicy.summarize(s, command, List.of());
    assertThat(result.compileMessage()).hasSize(10000);
    assertThat(result.total()).isNull();
    assertThat(result.score()).isNull();
    assertThat(result.runtime()).isNull();
    assertThat(command.toString()).doesNotContain("private");
  }

  @Test
  void incompleteAndMisorderedGradingCannotBeFinalizedAsAccepted() {
    var s = state("JUDGING", 2);
    var command = new FinishCommand(s.id(), FinishKind.COMPLETE, null, null);
    assertThatThrownBy(() -> SubmissionFinalizationPolicy.summarize(s, command, List.of()))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                SubmissionFinalizationPolicy.summarize(
                    s, command, List.of(new Result(2, "AC", 1, 1), new Result(1, "AC", 1, 1))))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                SubmissionFinalizationPolicy.summarize(
                    state("COMPILING", null), command, List.of()))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                SubmissionFinalizationPolicy.summarize(
                    state("JUDGING", 1), command, List.of(new Result(1, "X", 1, 1))))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () ->
                SubmissionFinalizationPolicy.summarize(
                    state("PENDING", null),
                    new FinishCommand(s.id(), FinishKind.COMPILE_ERROR, null, "mapped"),
                    List.of()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void invalidInternalWritesAreRejectedAndSensitiveValuesHaveSafeStringForms() {
    UUID id = UUID.randomUUID();
    assertThatThrownBy(() -> new FinishCommand(id, FinishKind.SYSTEM_ERROR, "OTHER", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FinishCommand(id, FinishKind.COMPILE_ERROR, null, " "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FinishCommand(id, FinishKind.COMPLETE, "WORKER_TIMEOUT", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new FinishCommand(id, FinishKind.COMPLETE, null, "private"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseWrite(id, id, id, 0, false, "AC", 1, 1, null, null, false))
        .isInstanceOf(IllegalArgumentException.class);
    var write = new TestcaseWrite(id, id, id, 1, true, "WA", 1, 1, "private", "private", true);
    assertThat(write.toString()).doesNotContain("private");
    assertThat(new Token(id, id).toString()).doesNotContain(id.toString());
    assertThatThrownBy(
            () ->
                SubmissionFinalizationPolicy.summarize(
                    state("JUDGING", 1),
                    new FinishCommand(id, FinishKind.SYSTEM_ERROR, "QUEUE_UNAVAILABLE", null),
                    List.of()))
        .isInstanceOf(IllegalStateException.class);
  }
}
