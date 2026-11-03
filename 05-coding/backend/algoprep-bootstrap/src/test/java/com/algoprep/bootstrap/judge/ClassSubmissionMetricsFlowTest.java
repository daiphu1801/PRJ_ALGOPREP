package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.application.ports.in.GetClassStudentSubmissionMetricsUseCase;
import com.algoprep.judge.application.ports.out.ClassSubmissionMetricsCache;
import com.algoprep.judge.domain.metrics.ClassSubmissionMetricsCalculator;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/** Trusted internal P1: actual PostgreSQL scopes and real Redis TTL/fallback, no F1/F2 writes. */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
class ClassSubmissionMetricsFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired GetClassStudentSubmissionMetricsUseCase metrics;
  @Autowired ClassSubmissionMetricsCache cache;
  @Autowired JdbcTemplate jdbc;
  @Autowired StringRedisTemplate redis;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
  private UUID student;
  private UUID problem;
  private UUID classId;
  private Instant now;
  private ClassMetricsScope scope;

  @BeforeEach
  void fixture() {
    student = UUID.randomUUID();
    problem = UUID.randomUUID();
    classId = UUID.randomUUID();
    now = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class).toInstant();
    scope =
        new ClassMetricsScope(
            classId,
            List.of(new ClassMetricsScope.Student(student, ago(60), null)),
            List.of(new ClassMetricsScope.Assignment(problem, ago(30), null)),
            4);
  }

  private Instant ago(int days) {
    return now.minusSeconds(86400L * days);
  }

  private UUID submission(UUID user, UUID p, Instant at, String verdict, String score) {
    UUID id = UUID.randomUUID();
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(verdict);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,score_ratio,testcase_set_version_at_grading,
          submitted_at,finished_at)
        VALUES(?,?,?,'JAVA','STANDARD_IO','private-source-secret','EDITOR',?,?,?,1,
          CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        id,
        user,
        p,
        verdict,
        verdict.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        score == null ? null : new BigDecimal(score),
        at.toString(),
        terminal ? at.toString() : null);
    return id;
  }

  @Test
  void exactAssignmentAndEnrollmentBoundsExcludeSelfPracticeAndDoNotDuplicateOverlaps() {
    UUID left = UUID.randomUUID();
    UUID empty = UUID.randomUUID();
    UUID unattempted = UUID.randomUUID();
    var query = scopedBoundaries(left, empty, unattempted);
    submission(student, problem, ago(10).minusSeconds(1), "ACCEPTED", "1");
    submission(student, problem, ago(10), "WRONG_ANSWER", ".5");
    submission(student, problem, ago(6), "ACCEPTED", "1");
    submission(student, problem, ago(3).minusSeconds(1), "ACCEPTED", "1");
    submission(student, problem, ago(3), "ACCEPTED", "1");
    submission(student, UUID.randomUUID(), ago(5), "ACCEPTED", "1");
    submission(UUID.randomUUID(), problem, ago(5), "ACCEPTED", "1");
    submission(left, problem, ago(7).minusSeconds(1), "ACCEPTED", "1");
    submission(left, problem, ago(7), "WRONG_ANSWER", ".5");
    submission(left, problem, ago(5), "ACCEPTED", "1");
    submission(left, problem, ago(5).plusSeconds(1), "ACCEPTED", "1");
    var result = metrics.read(query);
    assertThat(result).hasSize(3);
    assertThat(result.get(0).acceptedRatio()).isEqualTo(2.0 / 3);
    assertThat(result.get(0).lastSubmittedAt()).isEqualTo(ago(3).minusSeconds(1));
    assertThat(result.get(1).acceptedRatio()).isEqualTo(.5);
    assertThat(result.get(1).lastSubmittedAt()).isEqualTo(ago(5));
    assertThat(result.get(2).streakDays()).isZero();
    assertThat(result.get(2).lastSubmittedAt()).isNull();
    assertThat(result.get(2).acceptedRatio()).isNull();
    assertThat(result.get(0).assignmentResults()).hasSize(1);
    assertThat(result.get(0).assignmentResults().getFirst().problemId()).isEqualTo(unattempted);
    assertThat(result.get(0).assignmentResults().getFirst().bestRatio()).isNull();
    assertThat(json.writeValueAsString(result))
        .doesNotContain("private-source-secret", "sourceCode", "testcase");
  }

  private ClassMetricsScope scopedBoundaries(UUID left, UUID empty, UUID unattempted) {
    return new ClassMetricsScope(
        classId,
        List.of(
            new ClassMetricsScope.Student(student, ago(20), null),
            new ClassMetricsScope.Student(left, ago(7), ago(5)),
            new ClassMetricsScope.Student(empty, ago(20), null)),
        List.of(
            new ClassMetricsScope.Assignment(problem, ago(10), ago(3)),
            new ClassMetricsScope.Assignment(problem, ago(9), ago(4)),
            new ClassMetricsScope.Assignment(problem, ago(10), ago(3)),
            new ClassMetricsScope.Assignment(unattempted, ago(2), null)),
        4);
  }

  @Test
  void completedIsoWeeksAndEligibleDenominatorComeFromActualPersistedStates() {
    var current = ClassSubmissionMetricsCalculator.weekStart(now);
    Instant last = current.minusWeeks(1).atStartOfDay().atOffset(ZoneOffset.ofHours(7)).toInstant();
    submission(student, problem, last, "ACCEPTED", "1");
    submission(student, problem, last.plusSeconds(1), "COMPILE_ERROR", null);
    submission(student, problem, last.plusSeconds(2), "SYSTEM_ERROR", null);
    submission(student, problem, last.plusSeconds(3), "JUDGING", null);
    submission(
        student,
        problem,
        current.atStartOfDay().atOffset(ZoneOffset.ofHours(7)).toInstant(),
        "ACCEPTED",
        "1");
    submission(student, problem, now.plusSeconds(86400), "ACCEPTED", "1");
    var result = metrics.read(scope).getFirst();
    assertThat(result.weeklyTrend()).hasSize(4);
    assertThat(result.weeklyTrend().getFirst().weekStart()).isEqualTo(current.minusWeeks(4));
    assertThat(result.weeklyTrend().getFirst().acceptedRatio()).isNull();
    assertThat(result.weeklyTrend().getLast().submissionCount()).isEqualTo(4);
    assertThat(result.weeklyTrend().getLast().acceptedCount()).isEqualTo(1);
    assertThat(result.weeklyTrend().getLast().acceptedRatio()).isEqualTo(.5);
    assertThat(result.acceptedRatio()).isEqualTo(2.0 / 3);
    assertThat(result.assignmentResults().getFirst().accepted()).isTrue();
  }

  @Test
  void activeAssignmentBestIgnoresGlobalScoreBeforeAssignmentAndChoosesLatestTie() {
    submission(student, problem, ago(31), "ACCEPTED", "1");
    submission(student, problem, ago(3), "WRONG_ANSWER", ".6");
    submission(student, problem, ago(2), "RUNTIME_ERROR", ".6");
    var result = metrics.read(scope).getFirst().assignmentResults().getFirst();
    assertThat(result.bestRatio()).isEqualByComparingTo(".6");
    assertThat(result.bestVerdict()).isEqualTo("RUNTIME_ERROR");
    assertThat(result.accepted()).isFalse();
  }

  @Test
  void actualRedisCacheIsBoundedToFifteenMinutesAndExpiryRestoresFreshReads() {
    assertThat(metrics.read(scope).getFirst().lastSubmittedAt()).isNull();
    String key = onlyKey();
    assertThat(redis.getExpire(key)).isBetween(1L, 900L);
    submission(student, problem, ago(1), "ACCEPTED", "1");
    assertThat(metrics.read(scope).getFirst().lastSubmittedAt()).isNull();
    redis.expire(key, Duration.ZERO);
    assertThat(metrics.read(scope).getFirst().acceptedRatio()).isEqualTo(1.0);
  }

  @Test
  void brokenRedisTypeAndMalformedJsonFallBackToDatabaseInsteadOfFalseZeros() {
    metrics.read(scope);
    String key = onlyKey();
    redis.delete(key);
    redis.opsForList().leftPush(key, "wrong-type");
    submission(student, problem, ago(1), "ACCEPTED", "1");
    assertThat(metrics.read(scope).getFirst().acceptedRatio()).isEqualTo(1.0);
    redis.opsForValue().set(key, "{private-invalid-cache-json");
    assertThat(metrics.read(scope).getFirst().acceptedRatio()).isEqualTo(1.0);
  }

  @Test
  void cacheKeysIncludeEveryWindowClassWeekCountAndCurrentIsoWeek() {
    var week = ClassSubmissionMetricsCalculator.weekStart(now);
    var result = metrics.read(scope);
    assertThat(cache.get(scope, week)).isPresent();
    var changedStudent =
        new ClassMetricsScope(
            classId,
            List.of(new ClassMetricsScope.Student(student, ago(59), null)),
            scope.assignments(),
            4);
    assertThat(cache.get(changedStudent, week)).isEmpty();
    var changedLeft =
        new ClassMetricsScope(
            classId,
            List.of(new ClassMetricsScope.Student(student, ago(60), ago(1))),
            scope.assignments(),
            4);
    assertThat(cache.get(changedLeft, week)).isEmpty();
    var changedAssignment =
        new ClassMetricsScope(
            classId,
            scope.students(),
            List.of(new ClassMetricsScope.Assignment(problem, ago(29), null)),
            4);
    assertThat(cache.get(changedAssignment, week)).isEmpty();
    var changedRemoval =
        new ClassMetricsScope(
            classId,
            scope.students(),
            List.of(new ClassMetricsScope.Assignment(problem, ago(30), ago(1))),
            4);
    assertThat(cache.get(changedRemoval, week)).isEmpty();
    assertThat(
            cache.get(
                new ClassMetricsScope(UUID.randomUUID(), scope.students(), scope.assignments(), 4),
                week))
        .isEmpty();
    assertThat(
            cache.get(
                new ClassMetricsScope(classId, scope.students(), scope.assignments(), 3), week))
        .isEmpty();
    assertThat(cache.get(scope, week.plusWeeks(1))).isEmpty();
    assertThat(cache.get(scope, week).orElseThrow()).isEqualTo(result);
  }

  @Test
  void emptyAssignmentsReturnEveryStudentWithNoSubmissionOrBusinessWrites() {
    submission(student, problem, ago(1), "ACCEPTED", "1");
    var before =
        jdbc.queryForMap("SELECT count(*) AS n,max(updated_at) AS u FROM judge.submissions");
    var result = metrics.read(new ClassMetricsScope(classId, scope.students(), List.of(), 12));
    assertThat(result).hasSize(1);
    assertThat(result.getFirst().weeklyTrend()).hasSize(12);
    assertThat(result.getFirst().weeklyTrend())
        .allSatisfy(week -> assertThat(week.submissionCount()).isZero());
    assertThat(result.getFirst().assignmentResults()).isEmpty();
    assertThat(result.getFirst().lastSubmittedAt()).isNull();
    assertThat(jdbc.queryForMap("SELECT count(*) AS n,max(updated_at) AS u FROM judge.submissions"))
        .isEqualTo(before);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM judge.outbox_events", Long.class))
        .isZero();
  }

  private String onlyKey() {
    var keys = redis.keys("judge:classmetrics:" + classId + ":*");
    assertThat(keys).hasSize(1);
    return keys.iterator().next();
  }
}
