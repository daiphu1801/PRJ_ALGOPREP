package com.algoprep.judge.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.InstructorReadWindow;
import com.algoprep.judge.domain.model.infra.InfraEvents.Filter;
import com.algoprep.judge.domain.model.infra.InfraEvents.Position;
import com.algoprep.judge.domain.model.metrics.AssignmentMetricScope;
import com.algoprep.judge.domain.model.metrics.AssignmentPeriod;
import com.algoprep.judge.domain.model.metrics.AssignmentSolveState;
import com.algoprep.judge.domain.model.metrics.ClassMetricsScope;
import com.algoprep.judge.domain.model.read.HistoryWindow;
import com.algoprep.judge.domain.model.read.InstructorReadFilter;
import com.algoprep.judge.domain.model.read.LearnerSubmissionCandidate;
import com.algoprep.judge.domain.model.read.SubmissionReadState;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Read-side repositories: submissions, learner lists, instructor scope, metrics, infra events. */
class JdbcReadRepositoriesTest {
  private final FakeJdbc db = new FakeJdbc();
  private final UUID id = UUID.randomUUID();
  private final UUID user = UUID.randomUUID();
  private final UUID problem = UUID.randomUUID();
  private final UUID clazz = UUID.randomUUID();
  private final Instant at = Instant.parse("2026-11-01T10:00:00Z");
  private final OffsetDateTime ts = OffsetDateTime.ofInstant(at, ZoneOffset.UTC);

  private Map<String, Object> readRow(Object finished) {
    return FakeJdbc.row0(
        "id",
        id,
        "user_id",
        user,
        "problem_id",
        problem,
        "language",
        "JAVA",
        "submission_model",
        "STANDARD_IO",
        "status",
        "ACCEPTED",
        "submitted_at",
        ts,
        "finished_at",
        finished,
        "source_code",
        "src",
        "compile_error_message",
        null,
        "passed_testcase_count",
        3,
        "total_testcase_count",
        3,
        "score_ratio",
        new BigDecimal("1"),
        "runtime_ms",
        11,
        "memory_kb",
        22,
        "expired",
        false);
  }

  @Test
  void submissionReadOwnedFindRecentMapNullableFinish() {
    var repo = new JdbcSubmissionReadRepository(db.provider());
    db.rows(List.of(readRow(ts))).rows(List.of(readRow(null))).none().rows(List.of(readRow(ts)));
    var owned = repo.owned(id, user).orElseThrow();
    assertThat(owned.finishedAt()).isEqualTo(at);
    assertThat(owned.passed()).isEqualTo(3);
    assertThat(db.lastSql()).endsWith("user_id=?");
    assertThat(repo.find(id).orElseThrow().finishedAt()).isNull();
    assertThat(repo.find(id)).isEmpty();
    assertThat(repo.recent(user, problem, 5)).hasSize(1);
    assertThat(db.lastParams().values()).contains(user, problem, 5);
  }

  @Test
  void submissionReadCasesBeatsStderr() {
    var repo = new JdbcSubmissionReadRepository(db.provider());
    var tc = UUID.randomUUID();
    db.row(
        "testcase_id",
        tc,
        "testcase_order",
        1,
        "verdict",
        "WA",
        "runtime_ms",
        4,
        "memory_kb",
        5,
        "is_sample",
        true,
        "actual",
        "out",
        "stderr",
        "err");
    var cases = repo.cases(id);
    assertThat(cases.get(0).id()).isEqualTo(tc);
    assertThat(cases.get(0).sample()).isTrue();
    assertThat(cases.get(0).stderr()).isEqualTo("err");
    db.row("total", 10L, "slower", 4L);
    var state =
        new SubmissionReadState(
            id,
            user,
            problem,
            "JAVA",
            "STANDARD_IO",
            "ACCEPTED",
            at,
            at,
            "s",
            null,
            1,
            1,
            BigDecimal.ONE,
            11,
            22,
            false);
    var beats = repo.beats(state);
    assertThat(beats.total()).isEqualTo(10L);
    assertThat(beats.slower()).isEqualTo(4L);
    db.row("x", true).none();
    assertThat(repo.stderrEnabled()).isTrue();
    assertThat(repo.stderrEnabled()).isFalse();
  }

  @Test
  void submissionReadCountAndHistoryBuildFilters() {
    var repo = new JdbcSubmissionReadRepository(db.provider());
    db.row("count", 7L);
    assertThat(repo.count(user, "ACCEPTED", "JAVA", Set.of(problem))).isEqualTo(7L);
    assertThat(db.lastSql()).contains("status=?", "language=?", "problem_id IN (?)");
    db.row("count", 0L);
    repo.count(user, null, null, Set.of());
    assertThat(db.lastSql()).contains("AND false");
    db.row("count", 2L);
    repo.count(user, null, null, null);
    assertThat(db.lastSql()).doesNotContain("problem_id", "status=");
    db.rows(List.of(readRow(ts)));
    var history = repo.history(user, null, "CPP", new HistoryWindow(null, 20, 10));
    assertThat(history).hasSize(1);
    assertThat(db.lastSql()).contains("OFFSET ? LIMIT ?");
    assertThat(db.lastParams().values()).contains(20, 10, "CPP");
  }

  @Test
  void learnerListsInProgressAndAccepted() {
    var repo = new JdbcLearnerSubmissionListRepository(db.provider());
    var row =
        FakeJdbc.row0(
            "id",
            id,
            "problem_id",
            problem,
            "language",
            "PYTHON",
            "status",
            "WRONG_ANSWER",
            "passed_testcase_count",
            1,
            "total_testcase_count",
            null,
            "runtime_ms",
            null,
            "memory_kb",
            null,
            "submitted_at",
            ts);
    db.rows(List.of(row)).rows(List.of(row)).rows(List.of(row));
    var first = repo.inProgress(user, null, 50);
    assertThat(first.get(0).totalCount()).isNull();
    assertThat(first.get(0).passedCount()).isEqualTo(1);
    assertThat(db.lastSql()).doesNotContain("CAST(? AS timestamptz)");
    repo.inProgress(user, new LearnerSubmissionCandidate.Cursor(at, id), 50);
    assertThat(db.lastSql()).contains("(submitted_at,id)<(CAST(");
    assertThat(db.lastParams().values()).contains(at.toString(), id);
    assertThat(repo.accepted(user, 3)).hasSize(1);
    assertThat(db.lastParams().values()).contains(user, 3);
  }

  @Test
  void instructorSubmissionsCountAndListWithEveryFilter() {
    var repo = new JdbcInstructorSubmissionRepository(db.provider());
    var window = new InstructorReadWindow(clazz, user, problem, at, null, at, null);
    var all =
        new InstructorReadFilter(
            List.of(window), clazz, id, "accepted", List.of(user), List.of(problem));
    db.row("c", 5L);
    assertThat(repo.count(all)).isEqualTo(5L);
    assertThat(db.lastSql())
        .contains("c.class_id=?", "s.id=?", "status='ACCEPTED'", "s.user_id IN (?)");
    var row =
        FakeJdbc.row0(
            "id",
            id,
            "class_id",
            clazz,
            "user_id",
            user,
            "problem_id",
            problem,
            "language",
            "JAVA",
            "status",
            "WRONG_ANSWER",
            "passed_testcase_count",
            1,
            "total_testcase_count",
            2,
            "score_ratio",
            new BigDecimal("0.5"),
            "submitted_at",
            ts);
    db.rows(List.of(row));
    var failed =
        new InstructorReadFilter(List.of(window), null, null, "failed", List.of(), List.of());
    var list = repo.list(failed, 10, 20);
    assertThat(list.get(0).score()).isEqualByComparingTo("0.5");
    assertThat(list.get(0).classId()).isEqualTo(clazz);
    assertThat(db.lastSql()).contains("'WRONG_ANSWER','TIME_LIMIT_EXCEEDED'", "false OR false");
    assertThat(db.lastParams().values()).contains(10, 20);
    db.row("c", 0L);
    repo.count(new InstructorReadFilter(List.of(), null, null, null, null, null));
    assertThat(db.lastSql()).doesNotContain("s.user_id IN");
  }

  @Test
  void assignmentMetricsDefaultsToZeroAndMergesRows() {
    var repo = new JdbcAssignmentMetricsRepository(db.provider());
    var window = new InstructorReadWindow(clazz, user, problem, at, null, at, null);
    var empty =
        repo.problemMetrics(new AssignmentMetricScope(List.of(), Set.of(clazz), Set.of(problem)));
    assertThat(empty.get(problem).submissionCount()).isZero();
    assertThat(empty.get(problem).acRatePercent()).isNull();
    assertThat(db.calls).isEmpty();
    db.row("problem_id", problem, "total", 4L, "eligible", 4L, "accepted", 1L);
    var metrics =
        repo.problemMetrics(
            new AssignmentMetricScope(List.of(window), Set.of(clazz), Set.of(problem)));
    assertThat(metrics.get(problem).submissionCount()).isEqualTo(4L);
    assertThat(metrics.get(problem).acRatePercent()).isEqualTo(25.0);
  }

  @Test
  void assignmentStatesMapSolveState() {
    var repo = new JdbcAssignmentMetricsRepository(db.provider());
    assertThat(repo.assignmentStates(user, List.of())).isEmpty();
    db.row("class_id", clazz, "problem_id", problem, "assigned_at", ts, "state", "ATTEMPTED");
    var result =
        repo.assignmentStates(user, List.of(new AssignmentPeriod(clazz, problem, at, null)));
    assertThat(result)
        .containsEntry(
            new AssignmentPeriod.Key(clazz, problem, at), AssignmentSolveState.ATTEMPTED);
  }

  @Test
  void classMetricsNowAndFind() {
    var repo = new JdbcClassSubmissionMetricsRepository(db.provider());
    db.row("now", ts);
    assertThat(repo.now()).isEqualTo(at);
    var empty = new ClassMetricsScope(clazz, List.of(), List.of(), 4);
    assertThat(repo.find(empty, at)).isEmpty();
    var scope =
        new ClassMetricsScope(
            clazz,
            List.of(new ClassMetricsScope.Student(user, at, null)),
            List.of(new ClassMetricsScope.Assignment(problem, at, null)),
            4);
    db.row(
        "id",
        id,
        "user_id",
        user,
        "problem_id",
        problem,
        "status",
        "ACCEPTED",
        "score_ratio",
        BigDecimal.ONE,
        "submitted_at",
        ts);
    var found = repo.find(scope, at.plusSeconds(60));
    assertThat(found).hasSize(1);
    assertThat(found.get(0).verdict()).isEqualTo("ACCEPTED");
    assertThat(found.get(0).submittedAt()).isEqualTo(at);
  }

  @Test
  void adminStatsMapEveryAggregate() {
    var repo = new JdbcAdminSubmissionStatsRepository(db.provider());
    var asOf = at.plusSeconds(1);
    db.row("now", ts);
    assertThat(repo.now()).isEqualTo(at);
    db.row("total", 10L, "current_count", 4L, "previous_count", 3L);
    var totals = repo.totals(asOf);
    assertThat(totals.current()).isEqualTo(4L);
    assertThat(totals.previous()).isEqualTo(3L);
    db.row("day", LocalDate.of(2026, 11, 1), "n", 2L);
    assertThat(repo.daily(at, asOf).get(0).count()).isEqualTo(2L);
    db.row("day", LocalDate.of(2026, 11, 1), "language", "CPP", "n", 5L);
    assertThat(repo.languages(at, asOf).get(0).language()).isEqualTo("CPP");
    db.rows(
        List.of(
            FakeJdbc.row0("status", "ACCEPTED", "n", 3L),
            FakeJdbc.row0("status", "WRONG_ANSWER", "n", 1L)));
    assertThat(repo.verdicts(asOf)).containsEntry("ACCEPTED", 3L).containsEntry("WRONG_ANSWER", 1L);
    db.row("month", LocalDate.of(2026, 11, 1), "n", 9L);
    assertThat(repo.months(at, asOf).get(0).month()).isEqualTo(YearMonth.of(2026, 11));
    assertThat(repo.problems(Set.of(), asOf)).isEmpty();
    db.row("problem_id", problem, "n", 6L, "ac", 2L);
    var problems = repo.problems(Set.of(problem), asOf);
    assertThat(problems.get(0).acceptedCount()).isEqualTo(2L);
  }

  @Test
  void mySubmissionMetricsNowStreakLastDays() {
    var repo = new JdbcMySubmissionMetricsRepository(db.provider());
    db.row("now", ts).row("c", 3L).row("m", ts).none();
    assertThat(repo.now()).isEqualTo(at);
    assertThat(repo.streak(user, at)).isEqualTo(3);
    assertThat(repo.last(user, at)).isEqualTo(at);
    assertThat(repo.last(user, at)).isNull();
    db.row("day", LocalDate.of(2026, 11, 1), "n", 2L)
        .row("day", LocalDate.of(2026, 10, 30), "n", 1L);
    assertThat(repo.days(user, null, at)).hasSize(1);
    assertThat(db.lastSql()).doesNotContain("s.submitted_at>=?");
    var days = repo.days(user, at.minusSeconds(86400), at);
    assertThat(days.get(0).day()).isEqualTo(LocalDate.of(2026, 10, 30));
    assertThat(db.lastSql()).contains("s.submitted_at>=?");
  }

  @Test
  void mySubmissionMetricsTopicsDefaultsAndAggregates() {
    var repo = new JdbcMySubmissionMetricsRepository(db.provider());
    var topic = UUID.randomUUID();
    var emptyTopic = UUID.randomUUID();
    var noPairs = repo.topics(user, Map.of(emptyTopic, Set.of()), null, at);
    assertThat(noPairs.get(emptyTopic).submissionCount()).isZero();
    assertThat(db.calls).isEmpty();
    db.row("topic_id", topic, "eligible", 5L, "ac", 2L, "last_at", ts);
    var links = new LinkedHashMap<UUID, Set<UUID>>();
    links.put(topic, Set.of(problem));
    links.put(emptyTopic, Set.of());
    var result = repo.topics(user, links, at.minusSeconds(10), at);
    assertThat(result.get(topic).acceptedCount()).isEqualTo(2L);
    assertThat(result.get(topic).acRate()).isEqualTo(0.4);
    assertThat(result.get(emptyTopic).lastSubmittedAt()).isNull();
    assertThat(db.lastParams().values())
        .anyMatch(v -> v instanceof String s && s.contains(problem.toString()));
    db.none();
    assertThat(repo.topics(user, links, null, at).get(topic).submissionCount()).isZero();
  }

  @Test
  void infraEventsSummaryAndListFilters() {
    var repo = new JdbcInfraEventRepository(db.provider(), 3, 5, 90);
    db.row("warns", 2L, "errors", 1L);
    var summary = repo.summary();
    assertThat(summary.warnCount()).isEqualTo(2L);
    assertThat(summary.windowHours()).isEqualTo(24);
    assertThat(db.lastParams().values()).contains(3, 5, 90);
  }

  @Test
  void infraEventsListFilters() {
    var repo = new JdbcInfraEventRepository(db.provider(), 3, 5, 90);
    var row =
        FakeJdbc.row0(
            "occurred_at",
            ts,
            "source_key",
            "o:1",
            "level",
            "WARN",
            "service",
            "outbox",
            "submission_id",
            id,
            "kind",
            "SubmissionGraded",
            "attempts",
            3);
    db.rows(List.of(row));
    var plain = new Filter("", "", "", null, null, null);
    var rows = repo.list(plain, Map.of());
    assertThat(rows.get(0).position()).isEqualTo(new Position(at, "o:1"));
    assertThat(rows.get(0).attempts()).isEqualTo(3);
    assertThat(db.lastSql()).doesNotContain("strpos");
    db.none();
    var full =
        new Filter("grading", "error", "go-judge", at.minusSeconds(5), at, new Position(at, "s:9"));
    assertThat(
            repo.list(
                full,
                Map.of(
                    "OUTBOX",
                    "t1",
                    "WORKER_TIMEOUT",
                    "t2",
                    "SANDBOX_FAULT",
                    "t3",
                    "QUEUE_UNAVAILABLE",
                    "t4",
                    "SPEC_UNAVAILABLE",
                    "t5")))
        .isEmpty();
    assertThat(db.lastSql())
        .contains("strpos", "lower(level)=?", "service=?", "(occurred_at,source_key");
    assertThat(db.lastParams().values())
        .contains("grading", "error", "go-judge", "s:9", "t1", "t2");
  }

  @Test
  void maintenanceCandidatesClaimAndCleanup() {
    var repo = new JdbcJudgeMaintenanceRepository(db.provider());
    db.rows(List.of(FakeJdbc.row0("id", id)));
    assertThat(repo.staleCandidates(10, 120)).containsExactly(id);
    assertThat(db.lastParams().values()).contains(120, 10);
    db.row("l", false);
    assertThat(repo.claimStale(id, 120)).isFalse();
    assertThat(db.calls).hasSize(2);
    db.row("l", true).row("l", false);
    assertThat(repo.claimStale(id, 120)).isFalse();
    db.row("l", true).row("l", true).row("p", false).row("id", id);
    assertThat(repo.claimStale(id, 120)).isTrue();
    assertThat(db.lastParams().values()).contains(false, 120);
    db.row("l", true).row("l", true).row("p", true).none();
    assertThat(repo.claimStale(id, 120)).isFalse();
    db.row("l", true);
    assertThat(repo.tryJobLock("JOB-JD-02")).isTrue();
    db.update(4).update(2);
    assertThat(repo.deletePublished(7, 100)).isEqualTo(4);
    assertThat(repo.deleteTestcaseDetails(90, 100)).isEqualTo(2);
    assertThat(db.lastParams().values()).contains(90, 100);
  }

  @Test
  void maintenancePauseFlagAndHeartbeat() {
    var repo = new JdbcJudgeMaintenanceRepository(db.provider());
    db.row("queue_paused", true);
    assertThat(repo.lockQueuePaused()).isTrue();
    assertThat(db.lastSql()).endsWith("FOR UPDATE");
    repo.changeQueuePaused(false);
    assertThat(db.lastParams()).containsValue(false);
    repo.refreshPendingHeartbeat();
    assertThat(db.lastSql()).contains("status='PENDING'");
    assertThatThrownBy(() -> repo.tryJobLock("x")).isInstanceOf(RuntimeException.class);
  }
}
