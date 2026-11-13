package com.algoprep.judge.domain.metrics;

import com.algoprep.judge.domain.model.SubmissionActivityDay;
import com.algoprep.judge.domain.model.SubmissionMetricRange;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class MySubmissionMetricsCalculator {
  private static final ZoneOffset ZONE = ZoneOffset.ofHours(7);

  private MySubmissionMetricsCalculator() {}

  public static Instant from(SubmissionMetricRange range, Instant now) {
    int days = range == SubmissionMetricRange.SEVEN_DAYS ? 6 : 29;
    return range == SubmissionMetricRange.ALL
        ? null
        : now.atOffset(ZONE)
            .toLocalDate()
            .minusDays(days)
            .atStartOfDay()
            .atOffset(ZONE)
            .toInstant();
  }

  public static List<SubmissionActivityDay> buckets(
      SubmissionMetricRange range, List<SubmissionActivityDay> rows, Instant now) {
    var today = now.atOffset(ZONE).toLocalDate();
    var days =
        rows.stream()
            .collect(Collectors.toMap(SubmissionActivityDay::day, SubmissionActivityDay::count));
    return switch (range) {
      case SEVEN_DAYS ->
          IntStream.range(0, 7)
              .mapToObj(i -> today.minusDays(6L - i))
              .map(day -> new SubmissionActivityDay(day, days.getOrDefault(day, 0L)))
              .toList();
      case THIRTY_DAYS -> thirtyDays(days, today);
      case ALL -> weeks(days, today);
    };
  }

  private static List<SubmissionActivityDay> thirtyDays(
      Map<LocalDate, Long> days, LocalDate today) {
    long[] counts = new long[14];
    LocalDate[] labels = new LocalDate[14];
    for (int ago = 0; ago < 30; ago++) {
      int index = 13 - ago * 14 / 30;
      LocalDate day = today.minusDays(ago);
      counts[index] += days.getOrDefault(day, 0L);
      if (labels[index] == null || day.isAfter(labels[index])) {
        labels[index] = day;
      }
    }
    return IntStream.range(0, 14)
        .mapToObj(i -> new SubmissionActivityDay(labels[i], counts[i]))
        .toList();
  }

  private static List<SubmissionActivityDay> weeks(Map<LocalDate, Long> days, LocalDate today) {
    if (days.isEmpty()) {
      return List.of();
    }
    var grouped =
        days.entrySet().stream()
            .collect(
                Collectors.groupingBy(
                    entry -> week(entry.getKey()), Collectors.summingLong(Map.Entry::getValue)));
    LocalDate first = grouped.keySet().stream().min(LocalDate::compareTo).orElseThrow();
    return first
        .datesUntil(week(today).plusWeeks(1), java.time.Period.ofWeeks(1))
        .map(start -> new SubmissionActivityDay(start, grouped.getOrDefault(start, 0L)))
        .toList();
  }

  private static LocalDate week(LocalDate date) {
    return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }
}
