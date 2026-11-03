package com.algoprep.judge.domain.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.AdminSubmissionStats.DayCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LanguageDay;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LevelProblems;
import com.algoprep.judge.domain.model.AdminSubmissionStats.MonthCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.ProblemCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.Totals;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AdminSubmissionStatsCalculatorTest {
  private final LocalDate today = LocalDate.parse("2026-11-01");

  @Test
  void summaryHasSignedRoundedDeltaAndNoDivisionByZero() {
    var days = AdminSubmissionStatsCalculator.days(List.of(), today, 7);
    var emptyPrevious = AdminSubmissionStatsCalculator.summary(new Totals(80, 30, 0), days);
    assertThat(emptyPrevious.value()).isEqualTo(80);
    assertThat(emptyPrevious.deltaPercent()).isNull();
    assertThat(emptyPrevious.deltaDirection()).isEqualTo("none");
    assertThat(emptyPrevious.sparklineSeries()).containsExactly(0L, 0L, 0L, 0L, 0L, 0L, 0L);
    var increased = AdminSubmissionStatsCalculator.summary(new Totals(80, 4, 3), days);
    assertThat(increased.deltaPercent()).isEqualByComparingTo("33.3");
    assertThat(increased.deltaDirection()).isEqualTo("up");
    var decreased = AdminSubmissionStatsCalculator.summary(new Totals(80, 1, 3), days);
    assertThat(decreased.deltaPercent()).isEqualByComparingTo("-66.7");
    assertThat(decreased.deltaDirection()).isEqualTo("down");
    assertThat(AdminSubmissionStatsCalculator.summary(new Totals(80, 3, 3), days).deltaDirection())
        .isEqualTo("none");
  }

  @Test
  void percentagesUseLargestRemainderAndNeverIncludeSystemOrPending() {
    var result =
        AdminSubmissionStatsCalculator.verdicts(
            Map.of(
                "ACCEPTED",
                1L,
                "WRONG_ANSWER",
                1L,
                "COMPILE_ERROR",
                1L,
                "SYSTEM_ERROR",
                100L,
                "PENDING",
                100L));
    assertThat(result.slices()).hasSize(5);
    assertThat(result.slices().getFirst().percent()).isEqualTo(34);
    assertThat(result.slices().stream().mapToInt(slice -> slice.percent()).sum()).isEqualTo(100);
    assertThat(result.slices().stream().mapToLong(slice -> slice.count()).sum()).isEqualTo(3);
    assertThat(AdminSubmissionStatsCalculator.verdicts(Map.of()).slices())
        .allSatisfy(slice -> assertThat(slice.percent()).isZero());
  }

  @Test
  void languageSeriesHaveTwentyLocalDatesAndExactlyTheThreeLockedLanguages() {
    var result =
        AdminSubmissionStatsCalculator.languages(
            List.of(
                new LanguageDay(today, "JAVA", 3),
                new LanguageDay(today.minusDays(19), "CPP", 2),
                new LanguageDay(today, "RUBY", 99)),
            today);
    assertThat(result.pointLabels()).hasSize(20).startsWith("13/10").endsWith("01/11");
    assertThat(result.series().stream().map(series -> series.languageCode()).toList())
        .containsExactly("JAVA", "CPP", "PYTHON");
    assertThat(result.series().get(0).values().getLast()).isEqualTo(3);
    assertThat(result.series().get(1).values().getFirst()).isEqualTo(2);
    assertThat(result.series().get(2).values()).containsOnly(0L);
    assertThat(AdminSubmissionStatsCalculator.today(Instant.parse("2026-10-31T17:00:00Z")))
        .isEqualTo(today);
    assertThat(AdminSubmissionStatsCalculator.dayStart(today))
        .isEqualTo(Instant.parse("2026-10-31T17:00:00Z"));
  }

  @Test
  void dailyAndMonthlyBucketsRemainCompleteAcrossYearBoundaries() {
    var daily =
        AdminSubmissionStatsCalculator.daily(
            AdminSubmissionStatsCalculator.days(List.of(new DayCount(today, 4)), today, 7));
    assertThat(daily.columns()).hasSize(7);
    assertThat(daily.weekTotal()).isEqualTo(4);
    assertThat(daily.dailyAverage()).isEqualTo(1);
    var months =
        AdminSubmissionStatsCalculator.months(
            List.of(new MonthCount(YearMonth.of(2025, 12), 7)), LocalDate.parse("2026-01-02"));
    assertThat(months.points()).hasSize(6);
    assertThat(months.points().getFirst().month()).isEqualTo(YearMonth.of(2025, 8));
    assertThat(months.points().getLast().month()).isEqualTo(YearMonth.of(2026, 1));
    assertThat(months.monthTotal()).isEqualTo(7);
  }

  @Test
  void difficultyPreservesCatalogOrderAndIncludesLevelsWithNoProblemsOrAttempts() {
    UUID first = UUID.randomUUID();
    UUID empty = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    var levels =
        List.of(new LevelProblems(first, Set.of(problem)), new LevelProblems(empty, Set.of()));
    assertThat(AdminSubmissionStatsCalculator.problemIds(levels)).containsExactly(problem);
    var result =
        AdminSubmissionStatsCalculator.difficulty(levels, List.of(new ProblemCount(problem, 3, 1)));
    assertThat(result.groups().getFirst().levelId()).isEqualTo(first);
    assertThat(result.groups().getFirst().submissions()).isEqualTo(3);
    assertThat(result.groups().getFirst().accepted()).isEqualTo(1);
    assertThat(result.groups().getLast().submissions()).isZero();
    assertThatThrownBy(
            () ->
                AdminSubmissionStatsCalculator.problemIds(
                    List.of(levels.getFirst(), levels.getFirst())))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                AdminSubmissionStatsCalculator.problemIds(
                    List.of(levels.getFirst(), new LevelProblems(empty, Set.of(problem)))))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void topFourUseUnsignedPostgresUuidOrderForEqualCountsAndCopyCollections() {
    var low = UUID.fromString("7fffffff-0000-4000-8000-000000000001");
    var high = UUID.fromString("80000000-0000-4000-8000-000000000001");
    var result =
        AdminSubmissionStatsCalculator.top(
            List.of(
                new ProblemCount(high, 3, 1),
                new ProblemCount(low, 3, 1),
                new ProblemCount(UUID.randomUUID(), 1, 1),
                new ProblemCount(UUID.randomUUID(), 1, 1),
                new ProblemCount(UUID.randomUUID(), 1, 0)));
    assertThat(result.items()).hasSize(4);
    assertThat(result.items().getFirst().problemId()).isEqualTo(low);
    assertThat(result.items().get(1).problemId()).isEqualTo(high);
    assertThatThrownBy(() -> result.items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
