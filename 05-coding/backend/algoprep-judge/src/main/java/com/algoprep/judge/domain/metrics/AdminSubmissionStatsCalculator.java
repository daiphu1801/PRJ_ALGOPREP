package com.algoprep.judge.domain.metrics;

import com.algoprep.judge.domain.model.AdminSubmissionStats;
import com.algoprep.judge.domain.model.AdminSubmissionStats.DayCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LanguageDay;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LanguageSeries;
import com.algoprep.judge.domain.model.AdminSubmissionStats.LevelProblems;
import com.algoprep.judge.domain.model.AdminSubmissionStats.MonthCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.ProblemCount;
import com.algoprep.judge.domain.model.AdminSubmissionStats.Totals;
import com.algoprep.judge.domain.model.AdminSubmissionStats.VerdictSlice;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public final class AdminSubmissionStatsCalculator {
  private static final List<String> VERDICTS =
      List.of("ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR", "COMPILE_ERROR");
  private static final List<String> LANGUAGES = List.of("JAVA", "CPP", "PYTHON");
  private static final ZoneOffset ZONE = ZoneOffset.ofHours(7);
  private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM", Locale.ROOT);

  private AdminSubmissionStatsCalculator() {}

  public static LocalDate today(Instant now) {
    return now.atOffset(ZONE).toLocalDate();
  }

  public static Instant dayStart(LocalDate day) {
    return day.atStartOfDay().atOffset(ZONE).toInstant();
  }

  public static List<DayCount> days(List<DayCount> rows, LocalDate today, int count) {
    var values = rows.stream().collect(Collectors.toMap(DayCount::day, DayCount::count));
    return IntStream.range(0, count)
        .mapToObj(index -> today.minusDays(count - 1L - index))
        .map(day -> new DayCount(day, values.getOrDefault(day, 0L)))
        .toList();
  }

  public static AdminSubmissionStats.Summary summary(Totals counts, List<DayCount> daily) {
    BigDecimal delta =
        counts.previous() == 0
            ? null
            : BigDecimal.valueOf(counts.current())
                .subtract(BigDecimal.valueOf(counts.previous()))
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(counts.previous()), 1, RoundingMode.HALF_UP);
    String direction =
        counts.previous() == 0 || counts.current() == counts.previous()
            ? "none"
            : counts.current() > counts.previous() ? "up" : "down";
    return new AdminSubmissionStats.Summary(
        counts.all(), delta, direction, daily.stream().map(DayCount::count).toList());
  }

  public static AdminSubmissionStats.Languages languages(List<LanguageDay> rows, LocalDate today) {
    var dates = IntStream.range(0, 20).mapToObj(i -> today.minusDays(19L - i)).toList();
    List<LanguageSeries> series = new ArrayList<>();
    for (String language : LANGUAGES) {
      var counts =
          rows.stream()
              .filter(row -> row.language().equals(language))
              .collect(Collectors.toMap(LanguageDay::day, LanguageDay::count));
      series.add(
          new LanguageSeries(
              language, dates.stream().map(date -> counts.getOrDefault(date, 0L)).toList()));
    }
    return new AdminSubmissionStats.Languages(dates.stream().map(LABEL::format).toList(), series);
  }

  public static AdminSubmissionStats.Verdicts verdicts(Map<String, Long> rows) {
    long total = VERDICTS.stream().mapToLong(key -> rows.getOrDefault(key, 0L)).sum();
    int[] percents = new int[5];
    List<BigInteger> remainders = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      BigInteger count = BigInteger.valueOf(rows.getOrDefault(VERDICTS.get(i), 0L));
      var divided =
          total == 0
              ? new BigInteger[] {BigInteger.ZERO, BigInteger.ZERO}
              : count
                  .multiply(BigInteger.valueOf(100))
                  .divideAndRemainder(BigInteger.valueOf(total));
      percents[i] = divided[0].intValueExact();
      remainders.add(divided[1]);
    }
    var order =
        IntStream.range(0, 5)
            .boxed()
            .sorted(
                Comparator.<Integer, BigInteger>comparing(remainders::get)
                    .reversed()
                    .thenComparingInt(Integer::intValue))
            .toList();
    int remaining = total == 0 ? 0 : 100 - java.util.Arrays.stream(percents).sum();
    for (int i = 0; i < remaining; i++) {
      percents[order.get(i)]++;
    }
    return new AdminSubmissionStats.Verdicts(
        IntStream.range(0, 5)
            .mapToObj(
                i ->
                    new VerdictSlice(
                        VERDICTS.get(i), rows.getOrDefault(VERDICTS.get(i), 0L), percents[i]))
            .toList());
  }

  public static Set<UUID> problemIds(List<LevelProblems> levels) {
    var levelIds = new HashSet<UUID>();
    var result = new HashSet<UUID>();
    for (var level : levels) {
      if (!levelIds.add(level.levelId())) {
        throw new IllegalArgumentException("Duplicate stats level");
      }
      for (UUID id : level.problemIds()) {
        if (!result.add(id)) {
          throw new IllegalArgumentException("Problem belongs to multiple levels");
        }
      }
    }
    return Set.copyOf(result);
  }

  public static AdminSubmissionStats.Difficulty difficulty(
      List<LevelProblems> levels, List<ProblemCount> rows) {
    var counts =
        rows.stream().collect(Collectors.toMap(ProblemCount::problemId, Function.identity()));
    return new AdminSubmissionStats.Difficulty(
        levels.stream()
            .map(
                level -> {
                  long total =
                      level.problemIds().stream()
                          .filter(counts::containsKey)
                          .mapToLong(id -> counts.get(id).submissionCount())
                          .sum();
                  long accepted =
                      level.problemIds().stream()
                          .filter(counts::containsKey)
                          .mapToLong(id -> counts.get(id).acceptedCount())
                          .sum();
                  return new AdminSubmissionStats.LevelCount(level.levelId(), total, accepted);
                })
            .toList());
  }

  public static AdminSubmissionStats.Days daily(List<DayCount> rows) {
    long total = rows.stream().mapToLong(DayCount::count).sum();
    long average =
        BigDecimal.valueOf(total)
            .divide(BigDecimal.valueOf(7), 0, RoundingMode.HALF_UP)
            .longValueExact();
    return new AdminSubmissionStats.Days(rows, total, average);
  }

  public static AdminSubmissionStats.Months months(List<MonthCount> rows, LocalDate today) {
    var counts = rows.stream().collect(Collectors.toMap(MonthCount::month, MonthCount::count));
    var current = YearMonth.from(today);
    var result =
        IntStream.range(0, 6)
            .mapToObj(index -> current.minusMonths(5L - index))
            .map(month -> new MonthCount(month, counts.getOrDefault(month, 0L)))
            .toList();
    return new AdminSubmissionStats.Months(
        result, result.stream().mapToLong(MonthCount::count).sum());
  }

  public static AdminSubmissionStats.Top top(List<ProblemCount> rows) {
    return new AdminSubmissionStats.Top(
        rows.stream()
            .sorted(
                Comparator.comparingLong(ProblemCount::submissionCount)
                    .reversed()
                    .thenComparing(row -> row.problemId().toString()))
            .limit(4)
            .toList());
  }
}
