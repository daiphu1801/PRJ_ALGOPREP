package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.domain.model.InstructorReadWindow;
import com.algoprep.judge.domain.model.infra.InfraEvents.Filter;
import com.algoprep.judge.domain.model.metrics.AssignmentMetricScope;
import com.algoprep.judge.domain.model.metrics.AssignmentPeriod;
import com.algoprep.judge.domain.model.metrics.AssignmentSolveState;
import com.algoprep.judge.domain.model.metrics.ClassMetricsScope;
import com.algoprep.judge.domain.model.queue.QueueMonitoring.JobSettings;
import com.algoprep.judge.domain.model.read.HistoryWindow;
import com.algoprep.judge.domain.model.read.InstructorReadFilter;
import com.algoprep.judge.domain.model.read.LearnerSubmissionCandidate;
import com.algoprep.judge.domain.model.submission.PendingSubmission.Owner;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Read repositories on real PostgreSQL: filters, keyset cursors, jsonb windows, recursive streak.
 */
class JdbcReadModelsPgTest extends PostgresTestBase {
  private final UUID user = UUID.randomUUID();
  private final UUID other = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final UUID problem2 = UUID.randomUUID();
  private final Instant now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);

  private Instant ago(Duration d) {
    return now.minus(d);
  }

  // ---- submission read ----

  @Test
  void ownedHidesOtherUsersSubmissionsAndFlagsExpiry() {
    var mine = submission(user, problem, "ACCEPTED", ago(Duration.ofDays(100)));
    var repo = new JdbcSubmissionReadRepository(provider());
    var state = repo.owned(mine, user).orElseThrow();
    assertThat(state.expired()).isTrue();
    assertThat(state.status()).isEqualTo("ACCEPTED");
    assertThat(state.finishedAt()).isNotNull();
    assertThat(repo.owned(mine, other)).isEmpty();
    assertThat(repo.find(mine)).isPresent();
    assertThat(repo.find(UUID.randomUUID())).isEmpty();
  }

  @Test
  void casesExposeSampleOutputOnlyAndStderrOnlyForFailedSamples() {
    var sub = submission(user, problem, "WRONG_ANSWER", ago(Duration.ofMinutes(5)));
    var repo = new JdbcSubmissionReadRepository(provider());
    var worker = new JdbcSubmissionWorkerRepository(provider());
    for (int i = 1; i <= 2; i++) {
      var testcase = UUID.randomUUID();
      var token =
          new com.algoprep.judge.domain.model.execution.WorkerRecords.Token(
              UUID.randomUUID(), UUID.randomUUID());
      worker.issue(sub, testcase, token);
      var state = worker.lock(sub).orElseThrow();
      worker.save(
          new com.algoprep.judge.domain.model.execution.WorkerRecords.TestcaseWrite(
              sub,
              testcase,
              token.id(),
              i,
              i == 1,
              i == 1 ? "WA" : "AC",
              3,
              4,
              "out" + i,
              "err" + i,
              true),
          state,
          UUID.randomUUID());
    }
    var cases = repo.cases(sub);
    assertThat(cases).extracting(c -> c.order()).containsExactly(1, 2);
    assertThat(cases.get(0).actual()).isEqualTo("out1");
    assertThat(cases.get(0).stderr()).isEqualTo("err1");
    assertThat(cases.get(1).actual()).isNull();
    assertThat(cases.get(1).stderr()).isNull();
  }

  @Test
  void beatsCountsAcceptedPeersAndStderrFlagFollowsSettings() {
    var a = submission(user, problem, "ACCEPTED", ago(Duration.ofMinutes(9)));
    submission(other, problem, "ACCEPTED", ago(Duration.ofMinutes(8)));
    submission(other, problem, "WRONG_ANSWER", ago(Duration.ofMinutes(7)));
    jdbc.sql("UPDATE judge.submissions SET runtime_ms=10 WHERE id=:id").param("id", a).update();
    var repo = new JdbcSubmissionReadRepository(provider());
    var state = repo.find(a).orElseThrow();
    var beats = repo.beats(state);
    assertThat(beats.total()).isEqualTo(2);
    assertThat(beats.slower()).isZero();
    assertThat(repo.stderrEnabled()).isTrue();
    jdbc.sql("UPDATE judge.sandbox_settings SET return_stderr_to_student=false").update();
    assertThat(repo.stderrEnabled()).isFalse();
  }

  @Test
  void historyFiltersCountAndOrderNewestFirstWithOffset() {
    var repo = new JdbcSubmissionReadRepository(provider());
    var oldest = submission(user, problem, "ACCEPTED", "JAVA", ago(Duration.ofHours(3)), now);
    var middle = submission(user, problem2, "WRONG_ANSWER", "CPP", ago(Duration.ofHours(2)), now);
    var newest = submission(user, problem, "ACCEPTED", "CPP", ago(Duration.ofHours(1)), now);
    submission(other, problem, "ACCEPTED", "CPP", ago(Duration.ofHours(1)), now);
    assertThat(repo.count(user, null, null, null)).isEqualTo(3);
    assertThat(repo.count(user, "ACCEPTED", null, null)).isEqualTo(2);
    assertThat(repo.count(user, "ACCEPTED", "CPP", Set.of(problem))).isEqualTo(1);
    assertThat(repo.count(user, null, null, Set.of())).isZero();
    var page = repo.history(user, null, null, new HistoryWindow(null, 0, 2));
    assertThat(page).extracting(s -> s.id()).containsExactly(newest, middle);
    var rest = repo.history(user, null, null, new HistoryWindow(null, 2, 2));
    assertThat(rest).extracting(s -> s.id()).containsExactly(oldest);
    assertThat(repo.history(user, null, null, new HistoryWindow(Set.of(), 0, 5))).isEmpty();
    assertThat(repo.history(user, "WRONG_ANSWER", "CPP", new HistoryWindow(Set.of(problem2), 0, 5)))
        .extracting(s -> s.id())
        .containsExactly(middle);
    assertThat(repo.recent(user, problem, 1)).extracting(s -> s.id()).containsExactly(newest);
  }

  // ---- learner lists ----

  @Test
  void inProgressListsLatestNonAcceptedPerProblemWithKeysetCursor() {
    var repo = new JdbcLearnerSubmissionListRepository(provider());
    submission(user, problem, "WRONG_ANSWER", ago(Duration.ofHours(5)));
    var latestWrong = submission(user, problem, "RUNTIME_ERROR", ago(Duration.ofHours(4)));
    submission(user, problem2, "WRONG_ANSWER", ago(Duration.ofHours(6)));
    var third = UUID.randomUUID();
    submission(user, third, "WRONG_ANSWER", ago(Duration.ofHours(3)));
    submission(user, third, "ACCEPTED", ago(Duration.ofHours(2)));
    var all = repo.inProgress(user, null, 10);
    assertThat(all).hasSize(2);
    assertThat(all.get(0).submissionId()).isEqualTo(latestWrong);
    var next = repo.inProgress(user, all.get(0).cursor(), 10);
    assertThat(next).hasSize(1);
    assertThat(next.get(0).problemId()).isEqualTo(problem2);
    assertThat(repo.inProgress(user, null, 1)).hasSize(1);
    assertThat(repo.accepted(user, 5)).hasSize(1);
    assertThat(repo.accepted(other, 5)).isEmpty();
    assertThat(new LearnerSubmissionCandidate.Cursor(now, third).submissionId()).isEqualTo(third);
  }

  // ---- instructor and class scoped reads (jsonb_to_recordset write path) ----

  private InstructorReadWindow window(
      UUID cls, UUID student, UUID prob, Instant assigned, Instant removed) {
    return new InstructorReadWindow(
        cls, student, prob, ago(Duration.ofDays(30)), null, assigned, removed);
  }

  @Test
  void instructorScopeAttributesEachSubmissionToTheLatestAssignedClassOnly() {
    var repo = new JdbcInstructorSubmissionRepository(provider());
    var classA = UUID.randomUUID();
    var classB = UUID.randomUUID();
    var inside = submission(user, problem, "ACCEPTED", ago(Duration.ofDays(1)));
    var before = submission(user, problem, "WRONG_ANSWER", ago(Duration.ofDays(20)));
    submission(other, problem, "ACCEPTED", ago(Duration.ofDays(1)));
    var windows =
        List.of(
            window(classA, user, problem, ago(Duration.ofDays(10)), null),
            window(classB, user, problem, ago(Duration.ofDays(5)), null));
    var filter = new InstructorReadFilter(windows, null, null, null, null, null);
    assertThat(repo.count(filter)).isEqualTo(1);
    var rows = repo.list(filter, 0, 10);
    assertThat(rows).extracting(r -> r.id()).containsExactly(inside).doesNotContain(before);
    assertThat(rows.get(0).classId()).isEqualTo(classB);
    assertThat(repo.count(new InstructorReadFilter(windows, classA, null, null, null, null)))
        .isZero();
    assertThat(repo.count(new InstructorReadFilter(windows, null, inside, "accepted", null, null)))
        .isEqualTo(1);
    assertThat(repo.count(new InstructorReadFilter(windows, null, null, "failed", null, null)))
        .isZero();
    assertThat(
            repo.count(
                new InstructorReadFilter(windows, null, null, null, List.of(user), List.of())))
        .isEqualTo(1);
    assertThat(
            repo.count(
                new InstructorReadFilter(windows, null, null, null, List.of(), List.of(problem))))
        .isEqualTo(1);
    assertThat(
            repo.count(new InstructorReadFilter(windows, null, null, null, List.of(), List.of())))
        .isZero();
  }

  @Test
  void removedAssignmentWindowExcludesLaterSubmissions() {
    var repo = new JdbcInstructorSubmissionRepository(provider());
    var cls = UUID.randomUUID();
    submission(user, problem, "ACCEPTED", ago(Duration.ofDays(1)));
    var filter =
        new InstructorReadFilter(
            List.of(window(cls, user, problem, ago(Duration.ofDays(10)), ago(Duration.ofDays(2)))),
            null,
            null,
            null,
            null,
            null);
    assertThat(repo.count(filter)).isZero();
  }

  @Test
  void classMetricsOnlyReturnEnrolledStudentSubmissionsInsideAssignmentWindows() {
    var repo = new JdbcClassSubmissionMetricsRepository(provider());
    var cls = UUID.randomUUID();
    var inside = submission(user, problem, "ACCEPTED", ago(Duration.ofDays(1)));
    submission(user, problem2, "ACCEPTED", ago(Duration.ofDays(1)));
    submission(other, problem, "ACCEPTED", ago(Duration.ofDays(1)));
    var scope =
        new ClassMetricsScope(
            cls,
            List.of(new ClassMetricsScope.Student(user, ago(Duration.ofDays(30)), null)),
            List.of(new ClassMetricsScope.Assignment(problem, ago(Duration.ofDays(10)), null)),
            4);
    var found = repo.find(scope, repo.now());
    assertThat(found).extracting(s -> s.id()).containsExactly(inside);
    assertThat(found.get(0).verdict()).isEqualTo("ACCEPTED");
  }

  @Test
  void assignmentMetricsAggregatePerProblemAndSolveStates() {
    var repo = new JdbcAssignmentMetricsRepository(provider());
    var cls = UUID.randomUUID();
    submission(user, problem, "ACCEPTED", ago(Duration.ofDays(1)));
    submission(user, problem, "WRONG_ANSWER", ago(Duration.ofDays(1)));
    submission(user, problem, "PENDING", ago(Duration.ofDays(1)));
    submission(user, problem2, "WRONG_ANSWER", ago(Duration.ofDays(1)));
    var windows =
        List.of(
            window(cls, user, problem, ago(Duration.ofDays(10)), null),
            window(cls, user, problem2, ago(Duration.ofDays(10)), null));
    var metrics =
        repo.problemMetrics(
            new AssignmentMetricScope(windows, Set.of(cls), Set.of(problem, problem2)));
    assertThat(metrics.get(problem).submissionCount()).isEqualTo(3);
    assertThat(metrics.get(problem).acRatePercent()).isEqualTo(50.0);
    assertThat(metrics.get(problem2).acRatePercent()).isZero();
    var untouched = UUID.randomUUID();
    var periods =
        List.of(
            new AssignmentPeriod(cls, problem, ago(Duration.ofDays(10)), null),
            new AssignmentPeriod(cls, problem2, ago(Duration.ofDays(10)), null),
            new AssignmentPeriod(cls, untouched, ago(Duration.ofDays(10)), null));
    var states = repo.assignmentStates(user, periods);
    assertThat(states.get(periods.get(0).key())).isEqualTo(AssignmentSolveState.SOLVED);
    assertThat(states.get(periods.get(1).key())).isEqualTo(AssignmentSolveState.ATTEMPTED);
    assertThat(states.get(periods.get(2).key())).isEqualTo(AssignmentSolveState.UNSOLVED);
  }

  // ---- my metrics: recursive streak, days, topics ----

  private Instant vnNoon(LocalDate day) {
    return day.atTime(12, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
  }

  @Test
  void streakCountsConsecutiveDaysEndingAtTheReferenceDayAndStopsAtAGap() {
    var repo = new JdbcMySubmissionMetricsRepository(provider());
    var today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    submission(
        user, problem, "ACCEPTED", vnNoon(today.minusDays(0)).isAfter(now) ? now : vnNoon(today));
    submission(user, problem, "ACCEPTED", vnNoon(today.minusDays(1)));
    submission(user, problem, "ACCEPTED", vnNoon(today.minusDays(2)));
    submission(user, problem, "ACCEPTED", vnNoon(today.minusDays(4)));
    assertThat(repo.streak(user, now)).isEqualTo(3);
    assertThat(repo.streak(other, now)).isZero();
    assertThat(repo.last(user, now)).isNotNull();
    assertThat(repo.last(other, now)).isNull();
    assertThat(repo.now()).isAfterOrEqualTo(now);
  }

  @Test
  void daysGroupByLocalDateAndTopicsUseJsonbLinks() {
    var repo = new JdbcMySubmissionMetricsRepository(provider());
    var today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
    submission(user, problem, "ACCEPTED", vnNoon(today.minusDays(1)));
    submission(user, problem, "WRONG_ANSWER", vnNoon(today.minusDays(1)).plusSeconds(60));
    submission(user, problem2, "PENDING", vnNoon(today.minusDays(2)));
    var days = repo.days(user, null, now);
    assertThat(days)
        .extracting(d -> d.day())
        .containsExactly(today.minusDays(2), today.minusDays(1));
    assertThat(days.get(1).count()).isEqualTo(2);
    assertThat(repo.days(user, vnNoon(today.minusDays(1)).minusSeconds(60), now)).hasSize(1);
    var topic = UUID.randomUUID();
    var emptyTopic = UUID.randomUUID();
    var topics =
        repo.topics(
            user,
            Map.of(topic, Set.of(problem, problem2), emptyTopic, Set.of(UUID.randomUUID())),
            null,
            now);
    assertThat(topics.get(topic).submissionCount()).isEqualTo(2);
    assertThat(topics.get(topic).acceptedCount()).isEqualTo(1);
    assertThat(topics.get(topic).acRate()).isEqualTo(0.5);
    assertThat(topics.get(emptyTopic).submissionCount()).isZero();
    assertThat(
            repo.topics(user, Map.of(topic, Set.of(problem)), now.minusSeconds(5), now)
                .get(topic)
                .submissionCount())
        .isZero();
  }

  // ---- admin stats ----

  @Test
  void adminStatsAggregateTotalsDaysLanguagesVerdictsMonthsAndProblems() {
    var repo = new JdbcAdminSubmissionStatsRepository(provider());
    submission(user, problem, "ACCEPTED", "JAVA", ago(Duration.ofDays(1)), now);
    submission(user, problem, "WRONG_ANSWER", "CPP", ago(Duration.ofDays(2)), now);
    submission(other, problem2, "PENDING", "CPP", ago(Duration.ofDays(40)), now);
    var asOf = repo.now();
    var totals = repo.totals(asOf);
    assertThat(totals.all()).isEqualTo(3);
    assertThat(totals.current()).isEqualTo(2);
    assertThat(totals.previous()).isEqualTo(1);
    assertThat(repo.daily(ago(Duration.ofDays(7)), asOf)).hasSize(2);
    assertThat(repo.languages(ago(Duration.ofDays(7)), asOf))
        .extracting(l -> l.language())
        .containsExactlyInAnyOrder("JAVA", "CPP");
    assertThat(repo.verdicts(asOf))
        .containsEntry("ACCEPTED", 1L)
        .containsEntry("WRONG_ANSWER", 1L)
        .doesNotContainKey("PENDING");
    assertThat(repo.months(ago(Duration.ofDays(90)), asOf)).isNotEmpty();
    assertThat(repo.problems(Set.of(problem), asOf).get(0).acceptedCount()).isEqualTo(1);
    assertThat(repo.problems(Set.of(), asOf)).isEmpty();
  }

  // ---- queue monitor, infra events, failure, admission, worker job, snapshot ----

  @Test
  void queueMonitorCountsPendingAndRetainsRecentSystemErrorsOnly() {
    var repo = new JdbcQueueMonitorRepository(provider());
    submission(user, problem, "PENDING", "JAVA", ago(Duration.ofMinutes(10)), now);
    submission(user, problem, "JUDGING", "CPP", ago(Duration.ofMinutes(5)), now);
    var failed = submission(user, problem, "SYSTEM_ERROR", ago(Duration.ofMinutes(3)));
    var expired = submission(user, problem, "SYSTEM_ERROR", ago(Duration.ofDays(5)));
    jdbc.sql("UPDATE judge.submissions SET finished_at=:f WHERE id=:id")
        .param("f", utc(ago(Duration.ofDays(4))))
        .param("id", expired)
        .update();
    var facts = repo.snapshot();
    assertThat(facts.pendingCount()).isEqualTo(1);
    assertThat(facts.latencyCapSeconds()).isEqualTo(10);
    var settings = repo.jobSettings();
    assertThat(settings.pageSize()).isEqualTo(20);
    var counts = repo.counts(settings);
    assertThat(counts.waiting()).isEqualTo(1);
    assertThat(counts.failed()).isEqualTo(1);
    assertThat(counts.all()).isEqualTo(3);
    assertThat(repo.jobs("failed", settings, 0))
        .extracting(j -> j.submissionId())
        .containsExactly(failed);
    assertThat(repo.jobs("all", settings, 0)).hasSize(3);
    assertThat(repo.jobs("all", settings, 0).get(2).status()).isEqualTo("SYSTEM_ERROR");
    assertThat(repo.jobs("pending", new JobSettings(1, 24, settings.asOf()), 1)).isEmpty();
  }

  @Test
  void infraEventsReportOutboxRetriesAndSystemErrorsWithFilters() {
    infraEventsFixtureChecks();
  }

  private void retriedEvent(UUID sub, int attempts) {
    jdbc.sql(
            "INSERT INTO judge.outbox_events(id,aggregate_type,aggregate_id,event_type,payload,created_at,publish_attempts) VALUES(:id,'Submission',:s,'SubmissionGraded','{}'::jsonb,now(),:a)")
        .param("id", UUID.randomUUID())
        .param("s", sub)
        .param("a", attempts)
        .update();
  }

  private void infraEventsFixtureChecks() {
    var repo = new JdbcInfraEventRepository(provider(), 3, 5, 90);
    var sub = submission(user, problem, "PENDING", "JAVA", ago(Duration.ofHours(1)), now);
    submission(user, problem, "SYSTEM_ERROR", ago(Duration.ofHours(2)));
    retriedEvent(sub, 6);
    retriedEvent(sub, 3);
    retriedEvent(sub, 1);
    var summary = repo.summary();
    assertThat(summary.warnCount()).isEqualTo(1);
    assertThat(summary.errorCount()).isEqualTo(2);
    var templates =
        Map.of(
            "OUTBOX",
            "outbox {id} {kind} {attempts}",
            "WORKER_TIMEOUT",
            "t",
            "SANDBOX_FAULT",
            "sandbox fault {id}",
            "QUEUE_UNAVAILABLE",
            "q",
            "SPEC_UNAVAILABLE",
            "s");
    var all = repo.list(new Filter("", "", "", null, null, null), templates);
    assertThat(all).hasSize(3);
    assertThat(repo.list(new Filter("", "error", "outbox", null, null, null), templates))
        .hasSize(1);
    assertThat(repo.list(new Filter("", "", "go-judge", null, null, null), templates)).hasSize(1);
    assertThat(repo.list(new Filter("sandbox fault", "", "", null, null, null), templates))
        .hasSize(1);
    assertThat(repo.list(new Filter("nomatch", "", "", null, null, null), templates)).isEmpty();
    var first = all.get(0);
    var after = repo.list(new Filter("", "", "", null, null, first.position()), templates);
    assertThat(after).hasSize(2).doesNotContain(first);
    assertThat(
            repo.list(
                new Filter("", "", "", now.plusSeconds(3600), now.plusSeconds(7200), null),
                templates))
        .isEmpty();
  }

  @Test
  void queueFailureFinishPendingIsAtomicAndAppendsGradedEvent() {
    var repo = new JdbcQueueFailureRepository(provider());
    var pending = submission(user, problem, "PENDING", ago(Duration.ofMinutes(2)));
    var view = repo.finishPending(pending).orElseThrow();
    assertThat(view.status()).isEqualTo("SYSTEM_ERROR");
    assertThat(view.timing().finishedAt()).isNotNull();
    assertThat(repo.finishPending(pending)).isEmpty();
    assertThat(repo.find(pending).orElseThrow().status()).isEqualTo("SYSTEM_ERROR");
    assertThat(repo.firstAttempt(view)).isTrue();
    submission(user, problem, "PENDING", ago(Duration.ofMinutes(10)));
    assertThat(repo.firstAttempt(view)).isFalse();
    repo.appendGraded(UUID.randomUUID(), pending, "{\"x\":1}", now);
    assertThat(count("outbox_events")).isEqualTo(1);
    assertThat(
            jdbc.sql("SELECT payload->>'x' FROM judge.outbox_events").query(String.class).single())
        .isEqualTo("1");
  }

  @Test
  void admissionCountsOnlyTheLastHourForTheSamePair() {
    var repo = new JdbcSubmissionAdmissionRepository(provider());
    var owner = new Owner(user, problem);
    submission(user, problem, "PENDING", ago(Duration.ofMinutes(10)));
    submission(user, problem, "PENDING", ago(Duration.ofHours(2)));
    submission(user, problem2, "PENDING", ago(Duration.ofMinutes(10)));
    assertThat(repo.countRecent(owner, repo.databaseNow())).isEqualTo(1);
    assertThat(repo.languageEnabled("JAVA")).isTrue();
    jdbc.sql("UPDATE judge.language_configs SET enabled=false WHERE language='JAVA'").update();
    assertThat(repo.languageEnabled("JAVA")).isFalse();
    tx.executeWithoutResult(s -> repo.lockPair(owner));
    var id = UUID.randomUUID();
    repo.insert(
        new com.algoprep.judge.domain.model.submission.PendingSubmission(
            id,
            owner,
            new com.algoprep.judge.domain.model.submission.PendingSubmission.Program(
                "CPP", "STANDARD_IO", "int main(){}", "EDITOR"),
            2,
            ago(Duration.ofSeconds(1))));
    assertThat(
            jdbc.sql("SELECT status FROM judge.submissions WHERE id=:id")
                .param("id", id)
                .query(String.class)
                .single())
        .isEqualTo("PENDING");
  }

  @Test
  void workerJobSettingsControlSnapshotAndConfigRepositories() {
    var sub = submission(user, problem, "ACCEPTED", ago(Duration.ofMinutes(2)));
    var job = new JdbcWorkerJobRepository(provider()).find(sub).orElseThrow();
    assertThat(job.hasResults()).isFalse();
    assertThat(job.testcaseVersion()).isEqualTo(1);
    var settings = new JdbcWorkerJobRepository(provider()).settings("PYTHON");
    assertThat(settings.timeMultiplier()).isEqualByComparingTo("3.00");
    assertThat(settings.returnStderr()).isTrue();
    submission(user, problem, "PENDING", ago(Duration.ofMinutes(1)));
    var control = new JdbcWorkerJobRepository(provider()).control();
    assertThat(control.pending()).isEqualTo(1);
    assertThat(control.threshold()).isEqualTo(20);
    var snap = new JdbcSubmissionSnapshotRepository(provider()).find(sub).orElseThrow();
    assertThat(snap.snapshot().sourceCode()).isEqualTo("src");
    var pendingSnap = new JdbcSubmissionSnapshotRepository(provider());
    var pendingId = submission(user, problem, "WRONG_ANSWER", ago(Duration.ofMinutes(1)));
    assertThat(pendingSnap.find(pendingId).orElseThrow().snapshot()).isNull();
    var cfg = new JdbcJudgeConfigRepository(provider());
    assertThat(cfg.enabledLanguages()).containsExactly("CPP", "JAVA", "PYTHON");
    assertThat(cfg.generatorLimits().maxRuntimeSeconds()).isEqualTo(30);
    assertThat(cfg.multipliers("JAVA").orElseThrow().memoryMultiplier())
        .isEqualByComparingTo("2.00");
    assertThat(cfg.multipliers("GO")).isEmpty();
    assertThat(new JdbcSampleSettingsRepository(provider()).read())
        .extracting(s -> s.language())
        .containsExactly("CPP", "JAVA", "PYTHON");
    var defaults =
        new JdbcGradingDefaultsRepository(
                new org.springframework.beans.factory.ObjectProvider<
                    org.springframework.jdbc.core.JdbcTemplate>() {
                  @Override
                  public org.springframework.jdbc.core.JdbcTemplate getObject() {
                    return new org.springframework.jdbc.core.JdbcTemplate(data);
                  }
                })
            .read();
    assertThat(defaults.timeLimitMs()).isEqualTo(1000);
  }

  @Test
  void adminQueueSettingsRoundTrip() {
    var repo = new JdbcAdminQueueRepository(provider());
    var actor = UUID.randomUUID();
    tx.executeWithoutResult(
        s -> {
          var locked = repo.lock();
          assertThat(locked.paused()).isFalse();
          repo.saveSettings(Map.of("jobsPerPage", 50, "latencyCapSeconds", 7), actor);
          repo.setAutoscale(false, actor);
          repo.markPauseActor(actor);
        });
    var state = repo.read();
    assertThat(state.settings().jobsPerPage()).isEqualTo(50);
    assertThat(state.settings().latencyCapSeconds()).isEqualTo(7);
    assertThat(state.settings().errorRetentionHours()).isEqualTo(24);
    assertThat(state.autoscaleEnabled()).isFalse();
    assertThat(jdbc.sql("SELECT updated_by FROM judge.cluster_settings").query(UUID.class).single())
        .isEqualTo(actor);
  }
}
