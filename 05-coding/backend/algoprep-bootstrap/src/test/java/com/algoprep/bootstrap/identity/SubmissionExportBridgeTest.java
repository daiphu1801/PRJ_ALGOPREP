package com.algoprep.bootstrap.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.SubmissionReads.Filters;
import com.algoprep.judge.application.dto.SubmissionReads.History;
import com.algoprep.judge.application.dto.SubmissionReads.Page;
import com.algoprep.judge.application.dto.SubmissionReads.Problem;
import com.algoprep.judge.application.ports.in.ListMySubmissionHistoryUseCase;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** The paging loop and the field mapping of the submission export bridge, without a database. */
class SubmissionExportBridgeTest {
  private static final Problem PROBLEM =
      new Problem(UUID.randomUUID(), "P-1", "Two Sum", "s", "Easy", false);
  private final AtomicInteger calls = new AtomicInteger();

  private static History row(UUID id, Problem problem) {
    return new History(
        id, Instant.parse("2026-01-01T00:00:00Z"), problem, "JAVA", "ACCEPTED", 1, 1, 7);
  }

  /** A fake history of {@code total} rows served 20 per page, like the real handler. */
  private ListMySubmissionHistoryUseCase fake(int total) {
    List<History> all = new ArrayList<>();
    for (int i = 0; i < total; i++) {
      all.add(row(UUID.randomUUID(), PROBLEM));
    }
    return (owner, filters) -> {
      calls.incrementAndGet();
      int page = Integer.parseInt(filters.page());
      int from = Math.min((page - 1) * 20, total);
      return new Page<>(all.subList(from, Math.min(from + 20, total)), page, 20, total);
    };
  }

  @Test
  void everyPageIsReadAndTheRowsKeepTheirOrder() {
    var bridge = new SubmissionExportBridge(fake(45), Duration.ofSeconds(2));

    var export = bridge.readAll(UUID.randomUUID(), 2000);

    assertThat(export.rows()).hasSize(45);
    assertThat(export.truncated()).isFalse();
    assertThat(calls.get()).isEqualTo(3);
  }

  @Test
  void anExactMultipleOfThePageSizeDoesNotReadAnExtraPage() {
    var bridge = new SubmissionExportBridge(fake(40), Duration.ofSeconds(2));

    assertThat(bridge.readAll(UUID.randomUUID(), 2000).rows()).hasSize(40);
    assertThat(calls.get()).isEqualTo(2);
  }

  @Test
  void theCapStopsTheLoopAndFlagsTheCut() {
    var bridge = new SubmissionExportBridge(fake(50), Duration.ofSeconds(2));

    var export = bridge.readAll(UUID.randomUUID(), 30);

    assertThat(export.rows()).hasSize(30);
    assertThat(export.truncated()).isTrue();
    assertThat(calls.get()).isEqualTo(2);
  }

  @Test
  void aRowSeenOnTwoPagesIsKeptOnce() {
    UUID repeated = UUID.randomUUID();
    ListMySubmissionHistoryUseCase shifting =
        (owner, filters) ->
            Integer.parseInt(filters.page()) == 1
                ? new Page<>(
                    List.of(row(UUID.randomUUID(), PROBLEM), row(repeated, PROBLEM)), 1, 2, 4)
                : new Page<>(
                    List.of(row(repeated, PROBLEM), row(UUID.randomUUID(), PROBLEM)), 2, 2, 4);

    var export =
        new SubmissionExportBridge(shifting, Duration.ofSeconds(2)).readAll(UUID.randomUUID(), 10);

    assertThat(export.rows()).hasSize(3);
  }

  @Test
  void aMissingProblemGivesBlankProblemFieldsAndTheOtherFieldsSurvive() {
    ListMySubmissionHistoryUseCase noProblem =
        (owner, filters) -> new Page<>(List.of(row(UUID.randomUUID(), null)), 1, 20, 1);

    var line =
        new SubmissionExportBridge(noProblem, Duration.ofSeconds(2))
            .readAll(UUID.randomUUID(), 10)
            .rows()
            .get(0);

    assertThat(line.problemCode()).isNull();
    assertThat(line.problemTitle()).isNull();
    assertThat(line.status()).isEqualTo("ACCEPTED");
    assertThat(line.runtimeMs()).isEqualTo(7);
  }

  @Test
  void theCallerIdAndOnlyThePageNumberReachTheJudge() {
    UUID user = UUID.randomUUID();
    var seen = new ArrayList<Filters>();
    ListMySubmissionHistoryUseCase spy =
        (owner, filters) -> {
          assertThat(owner).isEqualTo(user);
          seen.add(filters);
          return new Page<>(List.of(), 1, 20, 0);
        };

    new SubmissionExportBridge(spy, Duration.ofSeconds(2)).readAll(user, 10);

    assertThat(seen).hasSize(1);
    assertThat(seen.get(0)).isEqualTo(new Filters(null, null, null, "1", null));
  }

  @Test
  void aJudgeFailureIsOnePlainErrorNotAPartialExport() {
    ListMySubmissionHistoryUseCase failing =
        (owner, filters) -> {
          throw new IllegalArgumentException("secret detail");
        };

    assertThatThrownBy(
            () ->
                new SubmissionExportBridge(failing, Duration.ofSeconds(2))
                    .readAll(UUID.randomUUID(), 10))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageNotContaining("secret");
  }
}
