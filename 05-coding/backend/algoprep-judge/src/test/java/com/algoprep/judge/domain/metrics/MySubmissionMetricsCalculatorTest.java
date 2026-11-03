package com.algoprep.judge.domain.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.algoprep.judge.domain.model.SubmissionActivityDay;
import com.algoprep.judge.domain.model.SubmissionMetricRange;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MySubmissionMetricsCalculatorTest {
  private final Instant now = Instant.parse("2026-11-04T05:00:00Z");
  private final LocalDate today = LocalDate.parse("2026-11-04");

  @Test
  void sevenDaysAreCompleteAndWindowStartsAtLocalMidnight() {
    var rows =
        List.of(
            new SubmissionActivityDay(today, 3), new SubmissionActivityDay(today.minusDays(6), 2));
    var result = MySubmissionMetricsCalculator.buckets(SubmissionMetricRange.SEVEN_DAYS, rows, now);
    assertThat(result).hasSize(7);
    assertThat(result.getFirst().count()).isEqualTo(2);
    assertThat(result.getLast().count()).isEqualTo(3);
    assertThat(result.get(1).count()).isZero();
    assertThat(MySubmissionMetricsCalculator.from(SubmissionMetricRange.SEVEN_DAYS, now))
        .isEqualTo(Instant.parse("2026-10-28T17:00:00Z"));
  }

  @Test
  void thirtyDaysProduceFourteenGroupsWithTheLastDateOfEachGroupAndPreserveTheTotal() {
    var rows =
        java.util.stream.IntStream.range(0, 30)
            .mapToObj(i -> new SubmissionActivityDay(today.minusDays(i), 1))
            .toList();
    var result =
        MySubmissionMetricsCalculator.buckets(SubmissionMetricRange.THIRTY_DAYS, rows, now);
    assertThat(result).hasSize(14);
    assertThat(result.stream().mapToLong(SubmissionActivityDay::count).sum()).isEqualTo(30);
    assertThat(result.getFirst().day()).isEqualTo(today.minusDays(28));
    assertThat(result.getFirst().count()).isEqualTo(2);
    assertThat(result.getLast().day()).isEqualTo(today);
    assertThat(result.getLast().count()).isEqualTo(3);
    assertThat(result.stream().filter(day -> day.count() == 3).count()).isEqualTo(2);
    assertThat(MySubmissionMetricsCalculator.from(SubmissionMetricRange.THIRTY_DAYS, now))
        .isEqualTo(Instant.parse("2026-10-05T17:00:00Z"));
  }

  @Test
  void allUsesMondayLabelsIncludesCurrentWeekAndDoesNotInventHistoryForAnEmptyAccount() {
    var rows =
        List.of(
            new SubmissionActivityDay(LocalDate.parse("2026-10-20"), 2),
            new SubmissionActivityDay(today, 1));
    var result = MySubmissionMetricsCalculator.buckets(SubmissionMetricRange.ALL, rows, now);
    assertThat(result).hasSize(3);
    assertThat(result.getFirst().day()).isEqualTo(LocalDate.parse("2026-10-19"));
    assertThat(result.get(1).count()).isZero();
    assertThat(result.getLast().day()).isEqualTo(LocalDate.parse("2026-11-02"));
    assertThat(result.getLast().count()).isEqualTo(1);
    assertThat(MySubmissionMetricsCalculator.from(SubmissionMetricRange.ALL, now)).isNull();
    assertThat(MySubmissionMetricsCalculator.buckets(SubmissionMetricRange.ALL, List.of(), now))
        .isEmpty();
  }
}
