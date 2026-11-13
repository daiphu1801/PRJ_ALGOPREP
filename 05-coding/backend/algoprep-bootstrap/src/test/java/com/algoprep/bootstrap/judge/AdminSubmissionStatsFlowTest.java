package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.application.ports.in.GetAdminDifficultyBreakdownUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByDayUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByLanguageUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsByMonthUseCase;
import com.algoprep.judge.application.ports.in.GetAdminSubmissionsSummaryUseCase;
import com.algoprep.judge.application.ports.in.GetAdminTopProblemsUseCase;
import com.algoprep.judge.application.ports.in.GetAdminVerdictDistributionUseCase;
import com.algoprep.judge.domain.metrics.AdminSubmissionStatsCalculator;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LevelProblems;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.json.JsonMapper;

/**
 * Actual grouped SQL for seven separate trusted inbound ports; no admin HTTP/F1 feature changes.
 */
@SpringBootTest(
    properties =
        "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration")
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AdminSubmissionStatsFlowTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired JdbcTemplate jdbc;
  @Autowired GetAdminSubmissionsSummaryUseCase summary;
  @Autowired GetAdminSubmissionsByLanguageUseCase languages;
  @Autowired GetAdminVerdictDistributionUseCase verdicts;
  @Autowired GetAdminDifficultyBreakdownUseCase difficulty;
  @Autowired GetAdminSubmissionsByDayUseCase days;
  @Autowired GetAdminSubmissionsByMonthUseCase months;
  @Autowired GetAdminTopProblemsUseCase top;
  private Instant now;
  private LocalDate today;
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  @BeforeEach
  void fixture() {
    // Each test transaction rolls back its fixtures in this private disposable database.
    now = jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class).toInstant();
    today = AdminSubmissionStatsCalculator.today(now);
  }

  private void submission(UUID problem, Instant at, String status, String language) {
    boolean terminal = !List.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.update(
        """
        INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,
          source_input_method,status,failure_reason,score_ratio,testcase_set_version_at_grading,
          submitted_at,finished_at)
        VALUES(?,?,?,?,'STANDARD_IO','private-source-secret','EDITOR',?,?,?,1,
          CAST(? AS timestamptz),CAST(? AS timestamptz))
        """,
        UUID.randomUUID(),
        UUID.randomUUID(),
        problem,
        language,
        status,
        status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null,
        status.equals("ACCEPTED") ? BigDecimal.ONE : null,
        at.toString(),
        terminal ? at.toString() : null);
  }

  @Test
  void allSevenPortsReturnHonestEmptyShapesWithoutMetadataFallbacks() {
    assertThat(summary.summary().value()).isZero();
    assertThat(summary.summary().deltaPercent()).isNull();
    assertThat(languages.byLanguage().pointLabels()).hasSize(20);
    assertThat(languages.byLanguage().series()).hasSize(3);
    assertThat(verdicts.verdicts().slices())
        .hasSize(5)
        .allSatisfy(s -> assertThat(s.count()).isZero());
    assertThat(
            difficulty
                .byDifficulty(List.of(new LevelProblems(UUID.randomUUID(), Set.of())))
                .groups()
                .getFirst()
                .submissions())
        .isZero();
    assertThat(days.byDay().columns()).hasSize(7);
    assertThat(days.byDay().dailyAverage()).isZero();
    assertThat(months.byMonth().points()).hasSize(6);
    assertThat(top.topProblems(Set.of()).items()).isEmpty();
  }

  @Test
  void verdictDistributionExcludesSystemAndInflightButSummaryCountsEveryState() {
    UUID problem = UUID.randomUUID();
    for (String status :
        List.of(
            "ACCEPTED",
            "ACCEPTED",
            "WRONG_ANSWER",
            "TIME_LIMIT_EXCEEDED",
            "RUNTIME_ERROR",
            "COMPILE_ERROR",
            "SYSTEM_ERROR",
            "PENDING",
            "JUDGING")) {
      submission(problem, now.minusSeconds(10), status, "JAVA");
    }
    var result = verdicts.verdicts();
    assertThat(result.slices().stream().mapToLong(s -> s.count()).sum()).isEqualTo(6);
    assertThat(result.slices().getFirst().count()).isEqualTo(2);
    assertThat(result.slices().stream().mapToInt(s -> s.percent()).sum()).isEqualTo(100);
    assertThat(result.slices().stream().map(s -> s.verdict()).toList())
        .containsExactly(
            "ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR", "COMPILE_ERROR");
    assertThat(summary.summary().value()).isEqualTo(9);
    assertThat(json.writeValueAsString(result))
        .doesNotContain("private-source-secret", "sourceCode", "testcase");
  }

  @Test
  void twentyLocalDaysThreeLanguagesAndSevenDailyBucketsUseUtcPlusSeven() {
    UUID problem = UUID.randomUUID();
    Instant midnight = AdminSubmissionStatsCalculator.dayStart(today.minusDays(2));
    submission(problem, midnight.minusSeconds(1), "PENDING", "JAVA");
    submission(problem, midnight, "ACCEPTED", "CPP");
    submission(
        problem,
        AdminSubmissionStatsCalculator.dayStart(today.minusDays(19)),
        "SYSTEM_ERROR",
        "PYTHON");
    submission(
        problem, AdminSubmissionStatsCalculator.dayStart(today.minusDays(20)), "ACCEPTED", "JAVA");
    var byLanguage = languages.byLanguage();
    assertThat(byLanguage.series().get(0).values().get(16)).isEqualTo(1);
    assertThat(byLanguage.series().get(1).values().get(17)).isEqualTo(1);
    assertThat(byLanguage.series().get(2).values().getFirst()).isEqualTo(1);
    assertThat(
            byLanguage.series().stream()
                .flatMap(s -> s.values().stream())
                .mapToLong(Long::longValue)
                .sum())
        .isEqualTo(3);
    var byDay = days.byDay();
    assertThat(byDay.columns().get(3).count()).isEqualTo(1);
    assertThat(byDay.columns().get(4).count()).isEqualTo(1);
    assertThat(byDay.weekTotal()).isEqualTo(2);
    assertThat(byDay.dailyAverage()).isZero();
    assertThat(summary.summary().sparklineSeries())
        .isEqualTo(byDay.columns().stream().map(c -> c.count()).toList());
  }

  @Test
  void sixMonthsIncludeCurrentMonthAndExcludeThePreviousWindow() {
    UUID problem = UUID.randomUUID();
    YearMonth current = YearMonth.from(today);
    Instant first = AdminSubmissionStatsCalculator.dayStart(current.minusMonths(5).atDay(1));
    submission(problem, first.minusSeconds(1), "ACCEPTED", "JAVA");
    submission(problem, first, "ACCEPTED", "JAVA");
    submission(problem, now.minusSeconds(1), "PENDING", "JAVA");
    var result = months.byMonth();
    assertThat(result.points()).hasSize(6);
    assertThat(result.points().getFirst().month()).isEqualTo(current.minusMonths(5));
    assertThat(result.points().getFirst().count()).isEqualTo(1);
    assertThat(result.points().getLast().month()).isEqualTo(current);
    assertThat(result.monthTotal()).isEqualTo(2);
  }

  @Test
  void summaryUsesAllTimeValueAndTwoRollingThirtyDayWindows() {
    UUID problem = UUID.randomUUID();
    submission(problem, now.minusSeconds(86400L * 61), "ACCEPTED", "JAVA");
    submission(problem, now.minusSeconds(86400L * 31), "SYSTEM_ERROR", "CPP");
    submission(problem, now.minusSeconds(30), "PENDING", "PYTHON");
    submission(problem, now.minusSeconds(20), "COMPILE_ERROR", "PYTHON");
    var result = summary.summary();
    assertThat(result.value()).isEqualTo(4);
    assertThat(result.deltaPercent()).isEqualByComparingTo("100.0");
    assertThat(result.deltaDirection()).isEqualTo("up");
    assertThat(result.sparklineSeries().stream().mapToLong(Long::longValue).sum()).isEqualTo(2);
  }

  @Test
  void topFiltersEligiblePublishedScopeBeforeLimitAndDifficultyKeepsEmptyLevels() {
    UUID excluded = UUID.randomUUID();
    for (int i = 0; i < 10; i++) {
      submission(excluded, now.minusSeconds(10), "ACCEPTED", "JAVA");
    }
    UUID low = UUID.fromString("7fffffff-0000-4000-8000-000000000001");
    UUID high = UUID.fromString("80000000-0000-4000-8000-000000000001");
    var candidates = new java.util.HashSet<UUID>();
    for (UUID id : List.of(low, high, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())) {
      candidates.add(id);
      submission(id, now.minusSeconds(10), "PENDING", "JAVA");
    }
    submission(low, now.minusSeconds(1), "ACCEPTED", "CPP");
    submission(high, now.minusSeconds(1), "WRONG_ANSWER", "CPP");
    var result = top.topProblems(candidates);
    assertThat(result.items()).hasSize(4);
    assertThat(result.items().getFirst().problemId()).isEqualTo(low);
    assertThat(result.items().get(1).problemId()).isEqualTo(high);
    assertThat(result.items())
        .noneSatisfy(item -> assertThat(item.problemId()).isEqualTo(excluded));
    UUID level = UUID.randomUUID();
    var groups =
        difficulty
            .byDifficulty(
                List.of(
                    new LevelProblems(level, Set.of(low, high)),
                    new LevelProblems(UUID.randomUUID(), Set.of(UUID.randomUUID())),
                    new LevelProblems(UUID.randomUUID(), Set.of())))
            .groups();
    assertThat(groups).hasSize(3);
    assertThat(groups.getFirst().levelId()).isEqualTo(level);
    assertThat(groups.getFirst().submissions()).isEqualTo(4);
    assertThat(groups.getFirst().accepted()).isEqualTo(1);
    assertThat(groups.get(1).submissions()).isZero();
    assertThat(groups.get(2).submissions()).isZero();
  }
}
