package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.exception.WorkerPersistenceException;
import com.algoprep.judge.domain.model.AdminQueueSettings;
import com.algoprep.judge.domain.model.PendingSubmission;
import com.algoprep.judge.domain.model.PendingSubmission.Owner;
import com.algoprep.judge.domain.model.PendingSubmission.Program;
import com.algoprep.judge.domain.model.QueueMonitoring.JobSettings;
import com.algoprep.judge.domain.model.SubmissionLifecycleView;
import com.algoprep.judge.domain.model.SubmissionLifecycleView.Timing;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/** Persistence mapping and parameter binding for the small F4 repositories. */
class JdbcSimpleRepositoriesTest {
  private final FakeJdbc db = new FakeJdbc();
  private final UUID id = UUID.randomUUID();
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final Instant at = Instant.parse("2026-11-01T10:00:00Z");
  private final OffsetDateTime ts = OffsetDateTime.ofInstant(at, ZoneOffset.UTC);

  @Test
  void admissionLanguageEnabledLockPairAndNow() {
    var repo = new JdbcSubmissionAdmissionRepository(db.provider());
    db.row("enabled", true).row("x", true).row("now", ts).row("c", 3L);
    assertThat(repo.languageEnabled("JAVA")).isTrue();
    repo.lockPair(new Owner(user, problem));
    assertThat(db.lastParams()).containsValue(user + ":" + problem);
    assertThat(repo.databaseNow()).isEqualTo(at);
    assertThat(repo.countRecent(new Owner(user, problem), at)).isEqualTo(3L);
    assertThat(db.lastParams()).containsValues(user, problem);
  }

  @Test
  void admissionInsertBindsEveryColumnAsPending() {
    var repo = new JdbcSubmissionAdmissionRepository(db.provider());
    var pending =
        new PendingSubmission(
            id,
            new Owner(user, problem),
            new Program("CPP", "STANDARD_IO", "int main(){}", "PASTE"),
            5,
            at);
    repo.insert(pending);
    assertThat(db.lastSql()).contains("'PENDING'");
    assertThat(db.lastParams().values())
        .contains(id, user, problem, "CPP", "STANDARD_IO", "int main(){}", "PASTE", 5);
  }

  @Test
  void workerJobFindMapsRow() {
    var repo = new JdbcWorkerJobRepository(db.provider());
    db.row(
        "id",
        id,
        "problem_id",
        problem,
        "language",
        "JAVA",
        "submission_model",
        "FUNCTION_WRAPPER",
        "source_code",
        "class A{}",
        "testcase_set_version_at_grading",
        3,
        "status",
        "PENDING",
        "has_results",
        false);
    var job = repo.find(id).orElseThrow();
    assertThat(job.testcaseVersion()).isEqualTo(3);
    assertThat(job.hasResults()).isFalse();
    assertThat(job.model()).isEqualTo("FUNCTION_WRAPPER");
    db.none();
    assertThat(repo.find(id)).isEmpty();
  }

  @Test
  void workerJobSettingsAndControlUseColumnIndexes() {
    var repo = new JdbcWorkerJobRepository(db.provider());
    db.row(
        "a",
        BigDecimal.ONE,
        "b",
        new BigDecimal("2.0"),
        "c",
        1000,
        "d",
        256,
        "e",
        64,
        "f",
        10,
        "g",
        false,
        "h",
        true,
        "i",
        true);
    var settings = repo.settings("PYTHON");
    assertThat(settings.timeMultiplier()).isEqualByComparingTo("1");
    assertThat(settings.memoryMultiplier()).isEqualByComparingTo("2");
    assertThat(settings.compileSeconds()).isEqualTo(10);
    assertThat(settings.processLimitEnabled()).isTrue();
    assertThat(settings.networkEnabled()).isFalse();
    db.row("a", true, "b", false, "c", 7, "d", 12L);
    var control = repo.control();
    assertThat(control.paused()).isTrue();
    assertThat(control.threshold()).isEqualTo(7);
    assertThat(control.pending()).isEqualTo(12L);
  }

  @Test
  void workerJobTranslatesMissingRowsToPersistenceException() {
    var repo = new JdbcWorkerJobRepository(db.provider());
    db.none().none();
    assertThatThrownBy(() -> repo.settings("JAVA")).isInstanceOf(WorkerPersistenceException.class);
    assertThatThrownBy(repo::control).isInstanceOf(WorkerPersistenceException.class);
  }

  @Test
  void queueFailureFinishPendingFindAndFirstAttempt() {
    var repo = new JdbcQueueFailureRepository(db.provider());
    var finished = OffsetDateTime.ofInstant(at.plusSeconds(5), ZoneOffset.UTC);
    var base =
        FakeJdbc.row0(
            "id",
            id,
            "user_id",
            user,
            "problem_id",
            problem,
            "language",
            "JAVA",
            "status",
            "SYSTEM_ERROR",
            "submitted_at",
            ts,
            "finished_at",
            finished);
    db.rows(List.of(base));
    var view = repo.finishPending(id).orElseThrow();
    assertThat(view.timing().finishedAt()).isEqualTo(at.plusSeconds(5));
    assertThat(view.owner()).isEqualTo(new Owner(user, problem));
    var pending = new LinkedHashMap<>(base);
    pending.put("finished_at", null);
    db.rows(List.of(pending)).none();
    assertThat(repo.find(id).orElseThrow().timing().finishedAt()).isNull();
    assertThat(repo.finishPending(id)).isEmpty();
    db.row("x", true);
    var lifecycle =
        new SubmissionLifecycleView(
            id, new Owner(user, problem), "JAVA", "PENDING", new Timing(at, null));
    assertThat(repo.firstAttempt(lifecycle)).isTrue();
    assertThat(db.lastParams()).containsValues(user, problem, id);
  }

  @Test
  void queueFailureAppendGradedWritesEnvelope() {
    var repo = new JdbcQueueFailureRepository(db.provider());
    var event = UUID.randomUUID();
    repo.appendGraded(event, id, "{\"a\":1}", at);
    assertThat(db.lastParams().values()).contains(event, id, "{\"a\":1}", ts);
    assertThat(db.lastSql()).contains("'SubmissionGraded'");
  }

  @Test
  void adminQueueReadLockAndWrites() {
    var repo = new JdbcAdminQueueRepository(db.provider());
    var row =
        FakeJdbc.row0(
            "queue_paused",
            true,
            "autoscale_enabled",
            false,
            "autoscale_job_threshold",
            9,
            "jobs_per_page",
            20,
            "error_retention_hours",
            48,
            "latency_cap_seconds",
            60,
            "updated_at",
            ts);
    db.rows(List.of(row)).rows(List.of(row));
    var state = repo.read();
    assertThat(state.paused()).isTrue();
    assertThat(state.settings().jobsPerPage()).isEqualTo(20);
    assertThat(state.settings().updatedAt()).isEqualTo(at);
    assertThat(db.lastSql()).doesNotContain("FOR UPDATE");
    repo.lock();
    assertThat(db.lastSql()).endsWith("FOR UPDATE");
    repo.markPauseActor(user);
    assertThat(db.lastParams()).containsValue(user);
    repo.setAutoscale(true, user);
    assertThat(db.lastParams().values()).contains(true, user);
  }

  @Test
  void adminQueueSaveSettingsMapsFieldsToColumnsAndRejectsUnknown() {
    var repo = new JdbcAdminQueueRepository(db.provider());
    repo.saveSettings(Map.of("jobsPerPage", 50), user);
    assertThat(db.lastSql()).contains("jobs_per_page=?");
    assertThat(db.lastParams().values()).contains(50, user);
    assertThatThrownBy(() -> repo.saveSettings(Map.of("evil", 1), user))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(new AdminQueueSettings(1, 2, 3, 4, at).values()).hasSize(4);
  }

  @Test
  void queueMonitorSnapshotSettingsAndCounts() {
    var repo = new JdbcQueueMonitorRepository(db.provider());
    db.row(
        "at",
        ts,
        "queue_paused",
        false,
        "autoscale_enabled",
        true,
        "autoscale_job_threshold",
        5,
        "pending",
        4L,
        "avg_wait",
        null,
        "latency_cap_seconds",
        30);
    var facts = repo.snapshot();
    assertThat(facts.pendingCount()).isEqualTo(4L);
    assertThat(facts.avgWaitMillis()).isNull();
    assertThat(facts.autoscaleEnabled()).isTrue();
    db.row("jobs_per_page", 10, "error_retention_hours", 24, "at", ts);
    var settings = repo.jobSettings();
    assertThat(settings).isEqualTo(new JobSettings(10, 24, at));
    db.row("waiting", 1L, "failed", 2L, "total", 3L);
    assertThat(repo.counts(settings).failed()).isEqualTo(2L);
    assertThat(db.lastParams().values()).contains(24);
  }

  @Test
  void queueMonitorJobsFilterByTab() {
    var repo = new JdbcQueueMonitorRepository(db.provider());
    var settings = new JobSettings(10, 24, at);
    var row =
        FakeJdbc.row0("id", id, "language", "JAVA", "status", "SYSTEM_ERROR", "waiting_ms", null);
    db.rows(List.of(row)).none().none();
    var jobs = repo.jobs("failed", settings, 20);
    assertThat(jobs).hasSize(1);
    assertThat(jobs.get(0).waitingMillis()).isNull();
    assertThat(db.lastSql()).contains("finished_at IS NOT NULL");
    assertThat(db.lastParams().values()).contains(10, 20L);
    assertThat(repo.jobs("all", settings, 0)).isEmpty();
    assertThat(db.lastSql()).contains("status IN ('PENDING','COMPILING','JUDGING') OR");
    assertThat(repo.jobs("pending", settings, 0)).isEmpty();
    assertThat(db.lastSql()).contains("WHERE").contains("status='PENDING' ORDER BY");
  }

  @Test
  void configRepositoryMapsLimitsLanguagesAndMultipliers() {
    var repo = new JdbcJudgeConfigRepository(db.provider());
    db.row("max_runtime_seconds", 10, "max_output_mb", 5);
    assertThat(repo.generatorLimits().maxOutputMb()).isEqualTo(5);
    db.rows(List.of(FakeJdbc.row0("language", "CPP"), FakeJdbc.row0("language", "JAVA")));
    assertThat(repo.enabledLanguages()).containsExactly("CPP", "JAVA");
    db.row(
        "time_limit_multiplier",
        new BigDecimal("1.5"),
        "memory_limit_multiplier",
        new BigDecimal("2"));
    assertThat(repo.multipliers("JAVA").orElseThrow().timeMultiplier()).isEqualByComparingTo("1.5");
    db.none();
    assertThat(repo.multipliers("RUBY")).isEmpty();
  }

  @Test
  void outboxLockPendingAndAttempted() {
    var repo = new JdbcJudgeOutboxRepository(db.provider());
    db.row("id", id, "event_type", "SubmissionGraded", "envelope", "{}", "publish_attempts", 2);
    var events = repo.lockPending();
    assertThat(events).hasSize(1);
    assertThat(events.get(0).attempts()).isEqualTo(2);
    assertThat(events.get(0).type()).isEqualTo("SubmissionGraded");
    repo.attempted(id, true);
    assertThat(db.lastParams().values()).contains(id, true);
    repo.attempted(id, false);
    assertThat(db.lastParams().values()).contains(false);
  }

  @Test
  void snapshotExposesSourceOnlyForAccepted() {
    var repo = new JdbcSubmissionSnapshotRepository(db.provider());
    db.row(
        "id",
        id,
        "user_id",
        user,
        "problem_id",
        problem,
        "language",
        "JAVA",
        "status",
        "ACCEPTED",
        "source_code",
        "code",
        "runtime_ms",
        12,
        "memory_kb",
        null);
    var accepted = repo.find(id).orElseThrow();
    assertThat(accepted.snapshot().sourceCode()).isEqualTo("code");
    assertThat(accepted.snapshot().memoryKb()).isNull();
    db.row("id", id, "status", "WRONG_ANSWER", "source_code", null);
    var wrong = repo.find(id).orElseThrow();
    assertThat(wrong.status()).isEqualTo("WRONG_ANSWER");
    assertThat(wrong.snapshot()).isNull();
    db.none();
    assertThat(repo.find(id)).isEmpty();
  }

  @Test
  void sampleSettingsReadsPositionalColumns() {
    var repo = new JdbcSampleSettingsRepository(db.provider());
    db.row(
        "l",
        "PYTHON",
        "e",
        true,
        "t",
        BigDecimal.ONE,
        "m",
        BigDecimal.TEN,
        "a",
        1000,
        "b",
        128,
        "c",
        64,
        "d",
        5,
        "n",
        false,
        "p",
        true,
        "s",
        false);
    var list = repo.read();
    assertThat(list).hasSize(1);
    assertThat(list.get(0).language()).isEqualTo("PYTHON");
    assertThat(list.get(0).enabled()).isTrue();
    assertThat(list.get(0).execution().defaultTimeMs()).isEqualTo(1000);
    assertThat(list.get(0).execution().processLimitEnabled()).isTrue();
  }

  @Test
  void gradingDefaultsReadsSingletonRow() {
    var fake =
        new FakeJdbc()
            .row(
                "time_limit_ms",
                1000,
                "memory_limit_mb",
                256,
                "output_limit_kb",
                64,
                "compile_timeout_sec",
                10);
    var template = new JdbcTemplate(fake.dataSourceForTemplate());
    ObjectProvider<JdbcTemplate> provider =
        new ObjectProvider<>() {
          @Override
          public JdbcTemplate getObject() {
            return template;
          }
        };
    var defaults = new JdbcGradingDefaultsRepository(provider).read();
    assertThat(defaults.timeLimitMs()).isEqualTo(1000);
    assertThat(defaults.compileTimeoutSeconds()).isEqualTo(10);
  }
}
