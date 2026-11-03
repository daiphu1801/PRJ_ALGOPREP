package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.application.dto.QueuePauseChange;
import com.algoprep.judge.application.dto.SubmissionProgress;
import com.algoprep.judge.application.ports.in.BeginSubmissionCompilationUseCase;
import com.algoprep.judge.application.ports.in.BeginSubmissionJudgingUseCase;
import com.algoprep.judge.application.ports.in.ChangeQueuePauseUseCase;
import com.algoprep.judge.application.ports.in.FinalizeSubmissionUseCase;
import com.algoprep.judge.application.ports.in.IssueSubmissionRunTokenUseCase;
import com.algoprep.judge.application.ports.in.PurgePublishedJudgeEventsUseCase;
import com.algoprep.judge.application.ports.in.PurgeTestcaseDetailsUseCase;
import com.algoprep.judge.application.ports.in.RecordSubmissionTestcaseUseCase;
import com.algoprep.judge.application.ports.in.SweepStaleSubmissionsUseCase;
import com.algoprep.judge.application.ports.out.SubmissionProgressPort;
import com.algoprep.judge.domain.model.WorkerRecords.FinishCommand;
import com.algoprep.judge.domain.model.WorkerRecords.FinishKind;
import com.algoprep.judge.domain.model.WorkerRecords.TestcaseWrite;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import com.algoprep.judge.domain.ports.out.WorkerLeasePort;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=8",
      "judge.maintenance.cleanup-batch-size=2"
    })
@ActiveProfiles("test")
@Testcontainers
@Import(JudgeMaintenanceFlowTest.Recording.class)
class JudgeMaintenanceFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired JdbcTemplate jdbc;
  @Autowired SweepStaleSubmissionsUseCase sweep;
  @Autowired PurgePublishedJudgeEventsUseCase purgeOutbox;
  @Autowired PurgeTestcaseDetailsUseCase purgeDetails;
  @Autowired ChangeQueuePauseUseCase queue;
  @Autowired JudgeMaintenanceRepository repository;
  @Autowired WorkerLeasePort leases;
  @Autowired PlatformTransactionManager manager;
  @Autowired BeginSubmissionCompilationUseCase compile;
  @Autowired BeginSubmissionJudgingUseCase judging;
  @Autowired IssueSubmissionRunTokenUseCase tokens;
  @Autowired RecordSubmissionTestcaseUseCase results;
  @Autowired FinalizeSubmissionUseCase finalizer;

  @TestConfiguration
  static class Recording {
    static final List<SubmissionProgress> EVENTS = new CopyOnWriteArrayList<>();

    @Bean
    @Primary
    SubmissionProgressPort maintenanceProgress() {
      return (owner, event) ->
          TransactionSynchronizationManager.registerSynchronization(
              new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                  EVENTS.add(event);
                }
              });
    }
  }

  @BeforeEach
  void fixture() {
    jdbc.update("DELETE FROM judge.outbox_events");
    jdbc.update("DELETE FROM judge.submission_testcase_results");
    jdbc.update("DELETE FROM judge.callback_logs");
    jdbc.update("DELETE FROM judge.judge_run_tokens");
    jdbc.update("DELETE FROM judge.submissions");
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=false");
    Recording.EVENTS.clear();
  }

  @Test
  void staleStatesFinishOnceWithAtomicGradedOutboxAndNoScore() {
    UUID pending = submission("PENDING", 301);
    UUID compiling = submission("COMPILING", 301);
    UUID judgingId = submission("JUDGING", 301);
    UUID recent = submission("JUDGING", 299);
    UUID terminal = submission("ACCEPTED", 600);
    assertThat(sweep.sweep()).isEqualTo(3);
    for (UUID id : List.of(pending, compiling, judgingId)) {
      assertThat(status(id)).isEqualTo("SYSTEM_ERROR");
      var row =
          jdbc.queryForMap(
              "SELECT failure_reason,score_ratio,finished_at,source_code FROM judge.submissions WHERE id=?",
              id);
      assertThat(row.get("failure_reason")).isEqualTo("WORKER_TIMEOUT");
      assertThat(row.get("score_ratio")).isNull();
      assertThat(row.get("finished_at")).isNotNull();
      assertThat(row.get("source_code")).isEqualTo("private source");
    }
    assertThat(status(recent)).isEqualTo("JUDGING");
    assertThat(status(terminal)).isEqualTo("ACCEPTED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE event_type='SubmissionGraded' AND payload->'payload'->>'verdict'='SYSTEM_ERROR' AND payload->'payload'->'passedTestcaseRatio'='null'::jsonb",
                Integer.class))
        .isEqualTo(3);
    assertThat(Recording.EVENTS).hasSize(3);
    assertThat(sweep.sweep()).isZero();
    assertThat(Recording.EVENTS).hasSize(3);
  }

  @Test
  void sweepHonorsItsBoundedBatchAndTheGlobalJobLock() throws Exception {
    for (int i = 0; i < 201; i++) {
      submission("PENDING", 600);
    }
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      new TransactionTemplate(manager)
          .executeWithoutResult(
              tx -> {
                assertThat(repository.tryJobLock("JOB-JD-01")).isTrue();
                try {
                  assertThat(executor.submit(sweep::sweep).get(10, TimeUnit.SECONDS)).isZero();
                } catch (Exception failure) {
                  throw new AssertionError(failure);
                }
              });
    }
    assertThat(sweep.sweep()).isEqualTo(200);
    assertThat(sweep.sweep()).isEqualTo(1);
    assertThat(sweep.sweep()).isZero();
  }

  @Test
  void pauseProtectsPendingButNotAbandonedCompilation() {
    UUID pending = submission("PENDING", 600);
    UUID abandoned = submission("COMPILING", 600);
    assertThat(queue.change(false, true)).isEqualTo(QueuePauseChange.CHANGED);
    assertThat(sweep.sweep()).isEqualTo(1);
    assertThat(status(pending)).isEqualTo("PENDING");
    assertThat(status(abandoned)).isEqualTo("SYSTEM_ERROR");
  }

  @Test
  void resumeAndHeartbeatCommitAndRollbackTogether() {
    UUID pending = submission("PENDING", 600);
    queue.change(false, true);
    var original = heartbeat(pending);
    new TransactionTemplate(manager)
        .executeWithoutResult(
            tx -> {
              assertThat(queue.change(true, false)).isEqualTo(QueuePauseChange.CHANGED);
              assertThat(heartbeat(pending)).isAfter(original);
              tx.setRollbackOnly();
            });
    assertThat(repository.lockQueuePaused()).isTrue();
    assertThat(heartbeat(pending)).isEqualTo(original);
    assertThat(queue.change(true, false)).isEqualTo(QueuePauseChange.CHANGED);
    assertThat(sweep.sweep()).isZero();
    var resumed = heartbeat(pending);
    assertThat(queue.change(false, false)).isEqualTo(QueuePauseChange.UNCHANGED);
    assertThat(queue.change(true, false)).isEqualTo(QueuePauseChange.CONFLICT);
    assertThat(heartbeat(pending)).isEqualTo(resumed);
    age(pending, 301);
    assertThat(sweep.sweep()).isEqualTo(1);
  }

  @Test
  void workerLeasePreventsSweepUntilTheWorkerConnectionIsGone() {
    UUID id = submission("JUDGING", 600);
    try (var lease = leases.acquire("judge:sub:" + id).orElseThrow()) {
      assertThat(sweep.sweep()).isZero();
      assertThat(status(id)).isEqualTo("JUDGING");
    }
    assertThat(sweep.sweep()).isEqualTo(1);
    assertThat(finalizer.finish(new FinishCommand(id, FinishKind.COMPLETE, null, null))).isEmpty();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE aggregate_id=?", Integer.class, id))
        .isEqualTo(1);
  }

  @Test
  void heartbeatCommittedWhileSweepWaitsOnTheRowWins() throws Exception {
    UUID id = submission("JUDGING", 600);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var task =
          new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Integer>>();
      new TransactionTemplate(manager)
          .executeWithoutResult(
              tx -> {
                jdbc.queryForObject(
                    "SELECT id FROM judge.submissions WHERE id=? FOR UPDATE", UUID.class, id);
                task.set(executor.submit(sweep::sweep));
                awaitDatabaseWait("%make_interval%");
                jdbc.update(
                    "UPDATE judge.submissions SET updated_at=clock_timestamp() WHERE id=?", id);
              });
      assertThat(task.get().get(10, TimeUnit.SECONDS)).isZero();
    }
    assertThat(status(id)).isEqualTo("JUDGING");
    assertThat(Recording.EVENTS).isEmpty();
  }

  @Test
  void pauseCommittedAfterCandidateSelectionIsRechecked() throws Exception {
    UUID id = submission("PENDING", 600);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var task =
          new java.util.concurrent.atomic.AtomicReference<java.util.concurrent.Future<Integer>>();
      new TransactionTemplate(manager)
          .executeWithoutResult(
              tx -> {
                jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true WHERE id=1");
                task.set(executor.submit(sweep::sweep));
                awaitDatabaseWait("%FOR SHARE%");
              });
      assertThat(task.get().get(10, TimeUnit.SECONDS)).isZero();
    }
    assertThat(status(id)).isEqualTo("PENDING");
  }

  @Test
  void outboxFailureRollsBackFinalStateAndProgress() {
    UUID id = submission("JUDGING", 600);
    jdbc.execute(
        "CREATE FUNCTION judge.reject_sweep() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'reject'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_sweep BEFORE INSERT ON judge.outbox_events FOR EACH ROW EXECUTE FUNCTION judge.reject_sweep()");
    try {
      assertThatThrownBy(sweep::sweep).isInstanceOf(RuntimeException.class);
      assertThat(status(id)).isEqualTo("JUDGING");
      assertThat(Recording.EVENTS).isEmpty();
    } finally {
      jdbc.execute("DROP TRIGGER reject_sweep ON judge.outbox_events");
      jdbc.execute("DROP FUNCTION judge.reject_sweep()");
    }
    assertThat(sweep.sweep()).isEqualTo(1);
  }

  @Test
  void exactTimeoutAndRetentionBoundariesUseTheDatabaseTransactionClock() {
    UUID id = submission("PENDING", 0);
    UUID event = outbox(91, true);
    new TransactionTemplate(manager)
        .executeWithoutResult(
            tx -> {
              jdbc.update(
                  "UPDATE judge.submissions SET updated_at=now()-interval '5 minutes' WHERE id=?",
                  id);
              assertThat(repository.claimStale(id, 300)).isFalse();
              jdbc.update(
                  "UPDATE judge.outbox_events SET published_at=now()-interval '90 days' WHERE id=?",
                  event);
              assertThat(repository.deletePublished(90, 2)).isZero();
            });
  }

  @Test
  void outboxCleanupDeletesAllBatchesButNeverPendingOrRecent() {
    for (int i = 0; i < 5; i++) {
      outbox(91, true);
    }
    UUID pending = outbox(120, false);
    UUID recent = outbox(89, true);
    assertThat(purgeOutbox.purge()).isEqualTo(5);
    assertThat(jdbc.queryForList("SELECT id FROM judge.outbox_events", UUID.class))
        .containsExactlyInAnyOrder(pending, recent);
    assertThat(purgeOutbox.purge()).isZero();
  }

  @Test
  void committedCleanupBatchSurvivesAFailureInTheNextBatch() {
    outbox(93, true);
    outbox(92, true);
    UUID failed = outbox(91, true);
    jdbc.execute(
        "CREATE FUNCTION judge.reject_cleanup() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'reject'; END $$");
    jdbc.execute(
        "CREATE TRIGGER reject_cleanup BEFORE DELETE ON judge.outbox_events FOR EACH ROW WHEN (OLD.id='"
            + failed
            + "'::uuid) EXECUTE FUNCTION judge.reject_cleanup()");
    try {
      assertThatThrownBy(purgeOutbox::purge).isInstanceOf(RuntimeException.class);
      assertThat(jdbc.queryForList("SELECT id FROM judge.outbox_events", UUID.class))
          .containsExactly(failed);
    } finally {
      jdbc.execute("DROP TRIGGER reject_cleanup ON judge.outbox_events");
      jdbc.execute("DROP FUNCTION judge.reject_cleanup()");
    }
    assertThat(purgeOutbox.purge()).isEqualTo(1);
  }

  @Test
  void testcaseCleanupKeepsSourceFinalScoreAndConsumedTokens() {
    UUID id = submission("PENDING", 0);
    compile.begin(id, 1);
    judging.beginJudging(id, 1);
    UUID testcase = UUID.randomUUID();
    var token = tokens.issue(id, testcase).orElseThrow();
    results.record(
        new TestcaseWrite(id, testcase, token.id(), 1, true, "AC", 1, 2, "1", null, false));
    finalizer.finish(new FinishCommand(id, FinishKind.COMPLETE, null, null));
    jdbc.update(
        "INSERT INTO judge.callback_logs(id,judge_run_token_id,raw_payload,outcome) VALUES(?,?,'{}'::jsonb,'ACCEPTED')",
        UUID.randomUUID(),
        token.id());
    verifyDetailRetentionBoundaries(id);
    jdbc.update(
        "UPDATE judge.submission_testcase_results SET created_at=now()-interval '91 days' WHERE submission_id=?",
        id);
    assertThat(purgeDetails.purge()).isEqualTo(1);
    assertThat(status(id)).isEqualTo("ACCEPTED");
    assertThat(
            jdbc.queryForObject(
                "SELECT source_code FROM judge.submissions WHERE id=?", String.class, id))
        .isEqualTo("private source");
    assertThat(
            jdbc.queryForObject(
                "SELECT score_ratio FROM judge.submissions WHERE id=?",
                java.math.BigDecimal.class,
                id))
        .isEqualByComparingTo("1");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.judge_run_tokens WHERE submission_id=? AND consumed_at IS NOT NULL",
                Integer.class,
                id))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.callback_logs WHERE judge_run_token_id=?",
                Integer.class,
                token.id()))
        .isEqualTo(1);
    assertThat(purgeDetails.purge()).isZero();
  }

  private void verifyDetailRetentionBoundaries(UUID id) {
    new TransactionTemplate(manager)
        .executeWithoutResult(
            tx -> {
              jdbc.update(
                  "UPDATE judge.submission_testcase_results SET created_at=now()-interval '90 days' WHERE submission_id=?",
                  id);
              assertThat(repository.deleteTestcaseDetails(90, 2)).isZero();
            });
    jdbc.update(
        "UPDATE judge.submission_testcase_results SET created_at=now()-interval '89 days' WHERE submission_id=?",
        id);
    assertThat(purgeDetails.purge()).isZero();
  }

  @Test
  void cleanupSkipsTheOtherInstancesTransactionLock() throws Exception {
    UUID event = outbox(91, true);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      new TransactionTemplate(manager)
          .executeWithoutResult(
              tx -> {
                assertThat(repository.tryJobLock("JOB-JD-03")).isTrue();
                try {
                  assertThat(executor.submit(purgeOutbox::purge).get(10, TimeUnit.SECONDS))
                      .isZero();
                } catch (Exception failure) {
                  throw new AssertionError(failure);
                }
              });
    }
    assertThat(jdbc.queryForList("SELECT id FROM judge.outbox_events", UUID.class))
        .containsExactly(event);
    assertThat(purgeOutbox.purge()).isEqualTo(1);
  }

  private UUID submission(String state, int ageSeconds) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,finished_at,testcase_set_version_at_grading) VALUES(?,?,?,'PYTHON','STANDARD_IO','private source','EDITOR',?,CASE WHEN ?='ACCEPTED' THEN now() ELSE NULL END,1)",
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        state,
        state);
    age(id, ageSeconds);
    return id;
  }

  private void age(UUID id, int seconds) {
    jdbc.update(
        "UPDATE judge.submissions SET updated_at=clock_timestamp()-make_interval(secs => ?) WHERE id=?",
        seconds,
        id);
  }

  private String status(UUID id) {
    return jdbc.queryForObject("SELECT status FROM judge.submissions WHERE id=?", String.class, id);
  }

  private java.time.Instant heartbeat(UUID id) {
    return jdbc.queryForObject(
            "SELECT updated_at FROM judge.submissions WHERE id=?", java.sql.Timestamp.class, id)
        .toInstant();
  }

  private UUID outbox(int days, boolean published) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at,published_at) VALUES(?,'Submission',?,'SubmissionGraded','{}'::jsonb,now()-make_interval(days => ?),CASE WHEN ? THEN now()-make_interval(days => ?) ELSE NULL END)",
        id,
        UUID.randomUUID(),
        days,
        published,
        days);
    return id;
  }

  private void awaitDatabaseWait(String query) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
    while (true) {
      jdbc.execute("SELECT pg_stat_clear_snapshot()");
      if (jdbc.queryForObject(
              "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type='Lock' AND query LIKE ?",
              Integer.class,
              query)
          > 0) {
        return;
      }
      assertThat(System.nanoTime()).isLessThan(deadline);
      try {
        Thread.sleep(25);
      } catch (InterruptedException failure) {
        Thread.currentThread().interrupt();
        throw new AssertionError(failure);
      }
    }
  }
}
