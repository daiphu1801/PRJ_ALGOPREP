package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.WorkerRecords.Outcome;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F4 worker persistence contract: every testcase is persisted, token is single-use. */
class JdbcSubmissionWorkerRepositoryTest {
  private final FakeJdbc db = new FakeJdbc();
  private final JdbcSubmissionWorkerRepository repo =
      new JdbcSubmissionWorkerRepository(db.provider());
  private final UUID id = UUID.randomUUID();
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant at = Instant.parse("2026-11-01T10:00:00Z");

  private Map<String, Object> submissionRow(String status) {
    var ts = OffsetDateTime.ofInstant(at, ZoneOffset.UTC);
    return FakeJdbc.row0(
        "id",
        id,
        "user_id",
        user,
        "problem_id",
        problem,
        "language",
        "PYTHON",
        "submission_model",
        "STANDARD_IO",
        "status",
        status,
        "total_testcase_count",
        4,
        "passed_testcase_count",
        2,
        "runtime_ms",
        30,
        "memory_kb",
        null,
        "submitted_at",
        ts,
        "updated_at",
        ts);
  }

  private State state(String model) {
    return new State(
        id, new Owner(user, problem), "PYTHON", model, "JUDGING", 4, 0, null, null, at, at);
  }

  private TestcaseWrite write(
      String verdict, boolean sample, String actual, String stderr, boolean ret) {
    return new TestcaseWrite(
        id, UUID.randomUUID(), UUID.randomUUID(), 2, sample, verdict, 5, 6, actual, stderr, ret);
  }

  @Test
  void compilingMapsStateAndPassesSpecVersion() {
    db.rows(java.util.List.of(submissionRow("COMPILING")));
    var state = repo.compiling(id, 7).orElseThrow();
    assertThat(state.status()).isEqualTo("COMPILING");
    assertThat(state.total()).isEqualTo(4);
    assertThat(state.passed()).isEqualTo(2);
    assertThat(state.memory()).isNull();
    assertThat(state.runtime()).isEqualTo(30);
    assertThat(state.submittedAt()).isEqualTo(at);
    assertThat(state.owner()).isEqualTo(new Owner(user, problem));
    assertThat(db.lastParams()).containsValues(id, 7);
  }

  @Test
  void compilingIsEmptyWhenAlreadyHasResultsOrTerminal() {
    db.none();
    assertThat(repo.compiling(id, 1)).isEmpty();
  }

  @Test
  void judgingHeartbeatAndLockReturnState() {
    db.rows(java.util.List.of(submissionRow("JUDGING")))
        .rows(java.util.List.of(submissionRow("JUDGING")))
        .rows(java.util.List.of(submissionRow("JUDGING")))
        .none();
    assertThat(repo.judging(id, 4).orElseThrow().status()).isEqualTo("JUDGING");
    assertThat(db.lastParams()).containsValues(id, 4);
    assertThat(repo.heartbeat(id)).isPresent();
    assertThat(repo.lock(id)).isPresent();
    assertThat(db.lastSql()).endsWith("FOR UPDATE");
    assertThat(repo.lock(id)).isEmpty();
  }

  @Test
  void resultExistsReadsBoolean() {
    db.row("exists", true).row("exists", false);
    var testcase = UUID.randomUUID();
    assertThat(repo.resultExists(id, testcase)).isTrue();
    assertThat(repo.resultExists(id, testcase)).isFalse();
  }

  @Test
  void issueReportsWhetherTokenWasInserted() {
    var token = new Token(UUID.randomUUID(), UUID.randomUUID());
    db.update(1).update(0);
    assertThat(repo.issue(id, UUID.randomUUID(), token)).isTrue();
    assertThat(db.lastParams()).containsValues(token.id(), token.value());
    assertThat(repo.issue(id, UUID.randomUUID(), token)).isFalse();
  }

  @Test
  void saveKeepsSampleSnippetTruncatedToCodePointsAndStderrOnlyForFailedStandardIo() {
    String longText = "a".repeat(4096) + "tail";
    var w = write("WA", true, longText, "boom", true);
    repo.save(w, state("STANDARD_IO"), UUID.randomUUID());
    assertThat(db.calls.get(0).params().values()).contains("a".repeat(4096), "boom");
    assertThat(db.calls.get(0).params().values()).doesNotContain(longText);
    assertThat(db.calls).hasSize(2);
  }

  @Test
  void saveDropsOutputForHiddenAndStderrForAcceptedOrFunctionWrapper() {
    repo.save(write("AC", false, "hidden", "err", true), state("STANDARD_IO"), UUID.randomUUID());
    assertThat(db.calls.get(0).params().values()).doesNotContain("hidden", "err");
    repo.save(
        write("WA", true, "shown", "err", true), state("FUNCTION_WRAPPER"), UUID.randomUUID());
    assertThat(db.calls.get(2).params().values()).contains("shown").doesNotContain("err");
    repo.save(write("WA", true, null, "err", false), state("STANDARD_IO"), UUID.randomUUID());
    assertThat(db.calls.get(4).params().values()).doesNotContain("err");
  }

  @Test
  void saveRejectsInvalidOrReusedToken() {
    db.update(0).update(1);
    assertThatThrownBy(
            () ->
                repo.save(
                    write("AC", true, "x", null, false), state("STANDARD_IO"), UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid run token");
    db.update(1).update(0);
    assertThatThrownBy(
            () ->
                repo.save(
                    write("AC", true, "x", null, false), state("STANDARD_IO"), UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void resultsAreMappedInOrder() {
    db.rows(
        java.util.List.of(
            FakeJdbc.row0("testcase_order", 1, "verdict", "AC", "runtime_ms", 3, "memory_kb", 10),
            FakeJdbc.row0(
                "testcase_order", 2, "verdict", "TLE", "runtime_ms", null, "memory_kb", null)));
    var results = repo.results(id);
    assertThat(results).hasSize(2);
    assertThat(results.get(0).runtime()).isEqualTo(3);
    assertThat(results.get(1).verdict()).isEqualTo("TLE");
    assertThat(results.get(1).memory()).isNull();
  }

  @Test
  void finishWritesOutcomeAndReturnsUpdatedState() {
    db.rows(java.util.List.of(submissionRow("ACCEPTED")));
    var outcome =
        new Outcome("ACCEPTED", 4, 4, new BigDecimal("1.0000"), 30, 100, null, null, null);
    var after = repo.finish(state("STANDARD_IO"), outcome);
    assertThat(after.status()).isEqualTo("ACCEPTED");
    assertThat(db.lastParams()).containsValues("ACCEPTED", 4, new BigDecimal("1.0000"));
  }

  @Test
  void finishFailsWhenSubmissionAlreadyTerminal() {
    db.none();
    var outcome =
        new Outcome("SYSTEM_ERROR", 0, null, null, null, null, "SANDBOX_FAULT", null, null);
    assertThatThrownBy(() -> repo.finish(state("STANDARD_IO"), outcome))
        .isInstanceOf(RuntimeException.class);
  }

  @Test
  void firstAttemptAndAppendUseUtcInstants() {
    db.row("x", true).row("x", false);
    assertThat(repo.firstAttempt(state("STANDARD_IO"))).isTrue();
    assertThat(db.lastParams()).containsValues(user, problem);
    assertThat(repo.firstAttempt(state("STANDARD_IO"))).isFalse();
    var event = UUID.randomUUID();
    repo.append(event, state("STANDARD_IO"), "SubmissionGraded", "{}");
    assertThat(db.lastParams()).containsValues(event, "SubmissionGraded", "{}");
    assertThat(db.lastParams().values())
        .anyMatch(v -> v instanceof OffsetDateTime o && o.toInstant().equals(at));
  }
}
