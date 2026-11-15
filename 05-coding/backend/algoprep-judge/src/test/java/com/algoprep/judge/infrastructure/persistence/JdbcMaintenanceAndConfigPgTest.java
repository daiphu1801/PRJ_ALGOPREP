package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Defaults;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Language;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Sandbox;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

/** Stale-submission sweep, outbox purge, outbox locking and admin config on real PostgreSQL. */
class JdbcMaintenanceAndConfigPgTest extends PostgresTestBase {
  private final JdbcJudgeMaintenanceRepository maintenance =
      new JdbcJudgeMaintenanceRepository(provider());
  private final JdbcJudgeOutboxRepository outbox = new JdbcJudgeOutboxRepository(provider());
  private final JdbcAdminLanguageConfigRepository config =
      new JdbcAdminLanguageConfigRepository(provider(), "javac", "g++", "py3");
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant old = Instant.now().minusSeconds(3600);

  private boolean claim(UUID id) {
    return Boolean.TRUE.equals(tx.execute(s -> maintenance.claimStale(id, 600)));
  }

  @Test
  void staleCandidatesOnlyContainOldActiveSubmissionsOldestFirst() {
    var stalePending = submission(user, problem, "PENDING", "JAVA", old, old);
    var staleJudging = submission(user, problem, "JUDGING", "CPP", old, old.minusSeconds(10));
    submission(user, problem, "JUDGING", "CPP", Instant.now(), Instant.now());
    submission(user, problem, "ACCEPTED", "CPP", old, old);
    assertThat(maintenance.staleCandidates(10, 600)).containsExactly(staleJudging, stalePending);
    assertThat(maintenance.staleCandidates(1, 600)).hasSize(1);
  }

  @Test
  void pausedQueueDoesNotExposePendingSubmissionsAsStale() {
    var pending = submission(user, problem, "PENDING", "JAVA", old, old);
    var judging = submission(user, problem, "JUDGING", "JAVA", old, old);
    maintenance.changeQueuePaused(true);
    assertThat(maintenance.lockQueuePaused()).isTrue();
    assertThat(maintenance.staleCandidates(10, 600))
        .containsExactly(judging)
        .doesNotContain(pending);
    maintenance.changeQueuePaused(false);
    assertThat(maintenance.staleCandidates(10, 600)).hasSize(2);
  }

  @Test
  void claimStaleSucceedsForAStaleRowAndFailsWhenFresh() {
    var stale = submission(user, problem, "COMPILING", "JAVA", old, old);
    var fresh = submission(user, problem, "COMPILING", "JAVA", Instant.now(), Instant.now());
    assertThat(claim(stale)).isTrue();
    assertThat(claim(fresh)).isFalse();
    assertThat(claim(UUID.randomUUID())).isFalse();
  }

  @Test
  void claimStaleRefusesPendingRowWhileQueueIsPaused() {
    var pending = submission(user, problem, "PENDING", "JAVA", old, old);
    maintenance.changeQueuePaused(true);
    assertThat(claim(pending)).isFalse();
  }

  @Test
  void transactionScopedAdvisoryLockStopsASecondSweeperAndIsReleasedOnCommit() {
    var stale = submission(user, problem, "JUDGING", "JAVA", old, old);
    var secondClaim = new AtomicBoolean(true);
    var jobLockSecond = new AtomicBoolean(true);
    tx.executeWithoutResult(
        s -> {
          assertThat(maintenance.claimStale(stale, 600)).isTrue();
          // A different connection (no shared transaction) must not obtain the same locks.
          try (var other = data.getConnection()) {
            var job =
                other.prepareStatement("SELECT pg_try_advisory_xact_lock(hashtext('JOB-JD-01'))");
            other.setAutoCommit(false);
            var rs = job.executeQuery();
            rs.next();
            jobLockSecond.set(rs.getBoolean(1));
            other.rollback();
          } catch (SQLException failure) {
            throw new IllegalStateException(failure);
          }
        });
    secondClaim.set(claim(stale));
    assertThat(jobLockSecond.get()).isFalse();
    assertThat(secondClaim.get()).isTrue();
  }

  @Test
  void tryJobLockIsExclusivePerTransaction() {
    tx.executeWithoutResult(
        s -> {
          assertThat(maintenance.tryJobLock("JOB-JD-09")).isTrue();
          try (var other = data.getConnection()) {
            other.setAutoCommit(false);
            var rs =
                other
                    .prepareStatement("SELECT pg_try_advisory_xact_lock(hashtext('JOB-JD-09'))")
                    .executeQuery();
            rs.next();
            assertThat(rs.getBoolean(1)).isFalse();
            other.rollback();
          } catch (SQLException failure) {
            throw new IllegalStateException(failure);
          }
        });
  }

  @Test
  void deletePublishedRemovesOnlyOldPublishedRowsInBatches() {
    var sub = submission(user, problem, "PENDING", "JAVA", old, old);
    for (int i = 0; i < 3; i++) {
      insertEvent(sub, "now() - interval '10 days'", i);
    }
    insertEvent(sub, "NULL", 0);
    insertEvent(sub, "now()", 0);
    assertThat(maintenance.deletePublished(7, 2)).isEqualTo(2);
    assertThat(maintenance.deletePublished(7, 2)).isEqualTo(1);
    assertThat(maintenance.deletePublished(7, 2)).isZero();
    assertThat(count("outbox_events")).isEqualTo(2);
  }

  private void insertEvent(UUID sub, String publishedAt, int attempts) {
    jdbc.sql(
            "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at,published_at,publish_attempts)"
                + " VALUES(:id,'Submission',:agg,'SubmissionGraded','{}'::jsonb,now(),"
                + publishedAt
                + ",:a)")
        .param("id", UUID.randomUUID())
        .param("agg", sub)
        .param("a", attempts)
        .update();
  }

  @Test
  void deleteTestcaseDetailsHonoursRetentionAndBatchSize() {
    var sub = submission(user, problem, "ACCEPTED", "JAVA", old, old);
    for (int i = 1; i <= 3; i++) {
      var tokenId = UUID.randomUUID();
      var testcase = UUID.randomUUID();
      jdbc.sql(
              "INSERT INTO judge.judge_run_tokens(id,submission_id,testcase_id,token,issued_at,consumed_at) VALUES(:i,:s,:t,:v,now(),now())")
          .param("i", tokenId)
          .param("s", sub)
          .param("t", testcase)
          .param("v", UUID.randomUUID())
          .update();
      jdbc.sql(
              "INSERT INTO judge.submission_testcase_results(id,submission_id,testcase_id,is_sample,testcase_order,verdict,runtime_ms,memory_kb,judge_run_token_id,created_at)"
                  + " VALUES(:id,:s,:t,false,:o,'AC',1,1,:tok, now() - (:age || ' days')::interval)")
          .param("id", UUID.randomUUID())
          .param("s", sub)
          .param("t", testcase)
          .param("o", i)
          .param("tok", tokenId)
          .param("age", i == 3 ? "1" : "100")
          .update();
    }
    assertThat(maintenance.deleteTestcaseDetails(90, 1)).isEqualTo(1);
    assertThat(maintenance.deleteTestcaseDetails(90, 10)).isEqualTo(1);
    assertThat(count("submission_testcase_results")).isEqualTo(1);
  }

  @Test
  void refreshPendingHeartbeatTouchesOnlyPendingRows() {
    var pending = submission(user, problem, "PENDING", "JAVA", old, old);
    var judging = submission(user, problem, "JUDGING", "JAVA", old, old);
    maintenance.refreshPendingHeartbeat();
    var fresh =
        jdbc.sql(
            "SELECT updated_at > now() - interval '1 minute' FROM judge.submissions WHERE id=:id");
    assertThat(fresh.param("id", pending).query(Boolean.class).single()).isTrue();
    assertThat(
            jdbc.sql(
                    "SELECT updated_at > now() - interval '1 minute' FROM judge.submissions WHERE id=:id")
                .param("id", judging)
                .query(Boolean.class)
                .single())
        .isFalse();
  }

  // ---- outbox ----

  @Test
  void lockPendingSkipsRowsLockedByAnotherTransactionAndOrdersByCreation() {
    var sub = submission(user, problem, "PENDING", "JAVA", old, old);
    var first = UUID.randomUUID();
    var second = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at) VALUES(:id,'Submission',:s,'SubmissionAccepted','{\"a\":1}'::jsonb,now()-interval '2 minutes')")
        .param("id", first)
        .param("s", sub)
        .update();
    jdbc.sql(
            "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at) VALUES(:id,'Submission',:s,'SubmissionGraded','{}'::jsonb,now())")
        .param("id", second)
        .param("s", sub)
        .update();
    tx.executeWithoutResult(
        s -> {
          var locked = outbox.lockPending();
          assertThat(locked).extracting(e -> e.id()).containsExactly(first, second);
          assertThat(locked.get(0).envelope()).contains("\"a\"");
          // Second transaction on its own connection: SKIP LOCKED returns nothing.
          var skipped = new java.util.concurrent.atomic.AtomicInteger(-1);
          var thread = new Thread(() -> skipped.set(tx.execute(t -> outbox.lockPending().size())));
          thread.start();
          try {
            thread.join();
          } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
          }
          assertThat(skipped.get()).isZero();
        });
    assertThat(outbox.lockPending()).hasSize(2);
  }

  @Test
  void attemptedCountsAttemptsAndPublishesOnlyWhenConfirmed() {
    var sub = submission(user, problem, "PENDING", "JAVA", old, old);
    var id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload) VALUES(:id,'Submission',:s,'SubmissionGraded','{}'::jsonb)")
        .param("id", id)
        .param("s", sub)
        .update();
    outbox.attempted(id, false);
    assertThat(
            jdbc.sql("SELECT publish_attempts FROM judge.outbox_events WHERE id=:id")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT published_at IS NULL FROM judge.outbox_events WHERE id=:id")
                .param("id", id)
                .query(Boolean.class)
                .single())
        .isTrue();
    outbox.attempted(id, true);
    assertThat(
            jdbc.sql("SELECT publish_attempts FROM judge.outbox_events WHERE id=:id")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(2);
    assertThat(outbox.lockPending()).isEmpty();
    outbox.attempted(id, true);
    assertThat(
            jdbc.sql("SELECT publish_attempts FROM judge.outbox_events WHERE id=:id")
                .param("id", id)
                .query(Integer.class)
                .single())
        .isEqualTo(2);
  }

  // ---- admin language config ----

  @Test
  void readReturnsSeededConfigWithPendingCounts() {
    submission(user, problem, "PENDING", "PYTHON", old, old);
    submission(user, problem, "PENDING", "PYTHON", old, old);
    submission(user, problem, "JUDGING", "PYTHON", old, old);
    var result = config.read();
    assertThat(result.languages())
        .extracting(Language::language)
        .containsExactly("CPP", "JAVA", "PYTHON");
    assertThat(result.languages().get(2).pendingJobCount()).isEqualTo(2);
    assertThat(result.languages().get(1).timeLimitMultiplier()).isEqualByComparingTo("2.00");
    assertThat(result.languages().get(1).compilerDisplayName()).isEqualTo("javac");
    assertThat(result.gradingDefaults().timeLimitMs()).isEqualTo(1000);
    assertThat(result.generatorSandbox().maxOutputMb()).isEqualTo(100);
    assertThat(result.sandbox().networkAccessEnabled()).isFalse();
  }

  @Test
  void lockHoldsRowLocksUntilTheTransactionEnds() {
    tx.executeWithoutResult(
        s -> {
          config.lock();
          assertThatThrownBy(() -> nowait("judge.language_configs"))
              .isInstanceOf(IllegalStateException.class)
              .hasMessage("55P03");
          assertThatThrownBy(() -> nowait("judge.sandbox_settings")).hasMessage("55P03");
        });
    nowait("judge.language_configs");
  }

  private void nowait(String table) {
    try (var connection = data.getConnection()) {
      connection.prepareStatement("SELECT * FROM " + table + " FOR UPDATE NOWAIT").executeQuery();
    } catch (SQLException failure) {
      throw new IllegalStateException(failure.getSQLState());
    }
  }

  @Test
  void saveChangesPersistsOnlyDifferingGroupsAndStampsActor() {
    var before = config.read();
    var actor = UUID.randomUUID();
    var languages =
        before.languages().stream()
            .map(
                l ->
                    l.language().equals("CPP")
                        ? new Language(
                            "CPP",
                            l.compilerDisplayName(),
                            new BigDecimal("1.50"),
                            l.memoryLimitMultiplier(),
                            false,
                            0,
                            l.updatedAt())
                        : l)
            .toList();
    var after =
        new AdminLanguageConfig(
            languages,
            new Defaults(2500, 256, 64, 10, before.gradingDefaults().updatedAt()),
            new Sandbox(true, true, true, before.sandbox().updatedAt()),
            before.generatorSandbox());
    tx.executeWithoutResult(s -> config.saveChanges(before, after, actor));
    var stored = config.read();
    assertThat(stored.languages().get(0).timeLimitMultiplier()).isEqualByComparingTo("1.50");
    assertThat(stored.languages().get(0).enabled()).isFalse();
    assertThat(stored.languages().get(1).enabled()).isTrue();
    assertThat(stored.gradingDefaults().timeLimitMs()).isEqualTo(2500);
    assertThat(stored.sandbox().networkAccessEnabled()).isTrue();
    assertThat(
            jdbc.sql("SELECT updated_by FROM judge.language_configs WHERE language='CPP'")
                .query(UUID.class)
                .single())
        .isEqualTo(actor);
    assertThat(
            jdbc.sql("SELECT updated_by IS NULL FROM judge.language_configs WHERE language='JAVA'")
                .query(Boolean.class)
                .single())
        .isTrue();
    assertThat(
            jdbc.sql("SELECT updated_by IS NULL FROM judge.generator_sandbox_settings")
                .query(Boolean.class)
                .single())
        .isTrue();
  }

  @Test
  void missingLanguageRowMakesTheConfigurationUnavailable() {
    jdbc.sql("DELETE FROM judge.language_configs WHERE language='CPP'").update();
    assertThatThrownBy(config::read).isInstanceOf(IllegalStateException.class);
  }
}
