package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.WorkerRecords.Outcome;
import com.algoprep.judge.domain.model.WorkerRecords.State;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.model.WorkerRecords.Token;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * F4 worker persistence against real PostgreSQL: single-use tokens, no fail-fast, atomic finish.
 */
class JdbcSubmissionWorkerRepositoryPgTest extends PostgresTestBase {
  private final JdbcSubmissionWorkerRepository repo =
      new JdbcSubmissionWorkerRepository(provider());
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant at = Instant.parse("2026-11-01T10:00:00Z");

  private UUID pending() {
    return submission(user, problem, "PENDING", at);
  }

  private TestcaseWrite write(Object... a) {
    // Order: submission, testcase, token, order, verdict, sample, actual output, stderr.
    return new TestcaseWrite(
        (UUID) a[0],
        (UUID) a[1],
        (UUID) a[2],
        (int) a[3],
        (boolean) a[5],
        (String) a[4],
        5,
        6,
        (String) a[6],
        (String) a[7],
        true);
  }

  private UUID issue(UUID sub, UUID testcase) {
    var token = new Token(UUID.randomUUID(), UUID.randomUUID());
    assertThat(repo.issue(sub, testcase, token)).isTrue();
    return token.id();
  }

  @Test
  void compilingResetsCountersAndSetsSpecVersion() {
    var id = pending();
    var state = repo.compiling(id, 4).orElseThrow();
    assertThat(state.status()).isEqualTo("COMPILING");
    assertThat(state.passed()).isZero();
    assertThat(state.total()).isNull();
    assertThat(
            jdbc.sql("SELECT spec_version FROM judge.submissions WHERE id=:id")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(4);
    assertThat(repo.compiling(id, 4)).isPresent();
  }

  @Test
  void compilingRefusesTerminalSubmissionsAndOnesWithResults() {
    var done = submission(user, problem, "ACCEPTED", at);
    assertThat(repo.compiling(done, 1)).isEmpty();
    var id = pending();
    var testcase = UUID.randomUUID();
    var token = issue(id, testcase);
    repo.save(write(id, testcase, token, 1, "AC", false, null, null), state(id), UUID.randomUUID());
    assertThat(repo.compiling(id, 1)).isEmpty();
    assertThat(repo.resultExists(id, testcase)).isTrue();
    assertThat(repo.resultExists(id, UUID.randomUUID())).isFalse();
  }

  private State state(UUID id) {
    return repo.lock(id).orElseThrow();
  }

  @Test
  void judgingNeedsCompilingStatusAndSpecVersion() {
    var id = pending();
    assertThat(repo.judging(id, 3)).isEmpty();
    repo.compiling(id, 2);
    var judging = repo.judging(id, 3).orElseThrow();
    assertThat(judging.status()).isEqualTo("JUDGING");
    assertThat(judging.total()).isEqualTo(3);
    assertThat(repo.heartbeat(id)).isPresent();
    assertThat(repo.heartbeat(pending())).isEmpty();
  }

  @Test
  void consumedTokenCannotBeReissuedNorReusedForASecondResult() {
    var id = pending();
    var testcase = UUID.randomUUID();
    var tokenId = issue(id, testcase);
    repo.save(
        write(id, testcase, tokenId, 1, "AC", false, null, null), state(id), UUID.randomUUID());
    assertThat(repo.issue(id, testcase, new Token(UUID.randomUUID(), UUID.randomUUID()))).isFalse();
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s ->
                        repo.save(
                            write(id, testcase, tokenId, 1, "AC", false, null, null),
                            state(id),
                            UUID.randomUUID())))
        .isInstanceOf(RuntimeException.class);
    assertThat(count("submission_testcase_results")).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT consumed_at IS NOT NULL FROM judge.judge_run_tokens WHERE id=:id")
                .param("id", tokenId)
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void saveWithForeignTokenIsRejectedAndLeavesNoResult() {
    var id = pending();
    var testcase = UUID.randomUUID();
    var other = issue(id, UUID.randomUUID());
    assertThatThrownBy(
            () ->
                tx.executeWithoutResult(
                    s ->
                        repo.save(
                            write(id, testcase, other, 1, "AC", false, null, null),
                            state(id),
                            UUID.randomUUID())))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("Invalid run token");
    assertThat(count("submission_testcase_results")).isZero();
    assertThat(
            jdbc.sql("SELECT count(*) FROM judge.judge_run_tokens WHERE consumed_at IS NOT NULL")
                .query(Integer.class)
                .single())
        .isZero();
  }

  @Test
  void sampleOutputIsCutAtFourThousandNinetySixCodePointsAndHiddenOutputIsNotStored() {
    var id = pending();
    String supplementary = new String(Character.toChars(0x20000));
    String long5000 = supplementary.repeat(5000);
    var sampleCase = UUID.randomUUID();
    var hiddenCase = UUID.randomUUID();
    var s = issue(id, sampleCase);
    var h = issue(id, hiddenCase);
    repo.save(
        write(id, sampleCase, s, 1, "WA", true, long5000, "trace"), state(id), UUID.randomUUID());
    repo.save(
        write(id, hiddenCase, h, 2, "WA", false, "secret", "secret-err"),
        state(id),
        UUID.randomUUID());
    var sample =
        jdbc.sql(
                "SELECT actual_output_snippet,stderr_snippet FROM judge.submission_testcase_results WHERE testcase_id=:t")
            .param("t", sampleCase)
            .query((rs, n) -> new String[] {rs.getString(1), rs.getString(2)})
            .single();
    assertThat(sample[0].codePointCount(0, sample[0].length())).isEqualTo(4096);
    assertThat(sample[1]).isEqualTo("trace");
    var hidden =
        jdbc.sql(
                "SELECT actual_output_snippet,stderr_snippet FROM judge.submission_testcase_results WHERE testcase_id=:t")
            .param("t", hiddenCase)
            .query((rs, n) -> new String[] {rs.getString(1), rs.getString(2)})
            .single();
    assertThat(hidden[0]).isNull();
    assertThat(hidden[1]).isNull();
  }

  @Test
  void everyTestcaseIsPersistedAfterAFailureAndResultsComeBackOrdered() {
    var id = pending();
    int[] order = {3, 1, 2};
    String[] verdicts = {"AC", "WA", "TLE"};
    for (int i = 0; i < 3; i++) {
      var testcase = UUID.randomUUID();
      var token = issue(id, testcase);
      repo.save(
          write(id, testcase, token, order[i], verdicts[i], false, null, null),
          state(id),
          UUID.randomUUID());
    }
    var results = repo.results(id);
    assertThat(results).extracting(r -> r.order()).containsExactly(1, 2, 3);
    assertThat(results).extracting(r -> r.verdict()).containsExactly("WA", "TLE", "AC");
  }

  @Test
  void finishWritesPartialScoreOnceAndRefusesASecondFinish() {
    var id = pending();
    var before = state(id);
    var outcome =
        new Outcome("WRONG_ANSWER", 2, 4, new BigDecimal("0.500"), 30, 1024, null, null, 1);
    var after = repo.finish(before, outcome);
    assertThat(after.status()).isEqualTo("WRONG_ANSWER");
    assertThat(after.passed()).isEqualTo(2);
    assertThat(
            jdbc.sql("SELECT score_ratio FROM judge.submissions WHERE id=:id")
                .param("id", id)
                .query(BigDecimal.class)
                .single())
        .isEqualByComparingTo("0.5");
    assertThatThrownBy(() -> repo.finish(before, outcome)).isInstanceOf(RuntimeException.class);
  }

  @Test
  void compileErrorAndSystemErrorOutcomesSatisfyTableConstraints() {
    var compile = pending();
    repo.finish(
        state(compile),
        new Outcome("COMPILE_ERROR", 0, null, null, null, null, null, "line 3: error", null));
    assertThat(
            jdbc.sql("SELECT compile_error_message FROM judge.submissions WHERE id=:id")
                .param("id", compile)
                .query(String.class)
                .single())
        .isEqualTo("line 3: error");
    var system = pending();
    repo.finish(
        state(system),
        new Outcome("SYSTEM_ERROR", 0, null, null, null, null, "WORKER_TIMEOUT", null, null));
    assertThat(
            jdbc.sql("SELECT failure_reason FROM judge.submissions WHERE id=:id")
                .param("id", system)
                .query(String.class)
                .single())
        .isEqualTo("WORKER_TIMEOUT");
  }

  @Test
  void firstAttemptIsTrueOnlyForTheEarliestSubmissionOfAUserProblemPair() {
    var first = submission(user, problem, "PENDING", at);
    var second = submission(user, problem, "PENDING", at.plusSeconds(60));
    assertThat(repo.firstAttempt(state(first))).isTrue();
    assertThat(repo.firstAttempt(state(second))).isFalse();
  }

  @Test
  void appendWritesJsonbOutboxEventAsUnpublished() {
    var id = pending();
    var event = UUID.randomUUID();
    repo.append(event, state(id), "SubmissionGraded", "{\"k\":\"v\"}");
    assertThat(
            jdbc.sql("SELECT payload->>'k' FROM judge.outbox_events WHERE id=:id")
                .param("id", event)
                .query(String.class)
                .single())
        .isEqualTo("v");
    assertThat(
            jdbc.sql("SELECT published_at IS NULL FROM judge.outbox_events WHERE id=:id")
                .param("id", event)
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void lockSerializesConcurrentWritersUntilTheTransactionEnds() {
    var id = pending();
    tx.executeWithoutResult(
        s -> {
          repo.lock(id);
          boolean blocked =
              jdbc.sql("SELECT EXISTS(SELECT 1 FROM judge.submissions WHERE id=:id)")
                  .param("id", id)
                  .query(Boolean.class)
                  .single();
          assertThat(blocked).isTrue();
          assertThatThrownBy(() -> lockNowaitOnSecondConnection(id))
              .isInstanceOf(RuntimeException.class);
        });
    lockNowaitOnSecondConnection(id);
  }

  private void lockNowaitOnSecondConnection(UUID id) {
    try (var connection = data.getConnection()) {
      var statement =
          connection.prepareStatement(
              "SELECT id FROM judge.submissions WHERE id=? FOR UPDATE NOWAIT");
      statement.setObject(1, id);
      statement.executeQuery();
    } catch (java.sql.SQLException failure) {
      throw new IllegalStateException(failure.getSQLState());
    }
  }
}
