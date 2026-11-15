package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Separate value objects for the existing seven admin dashboard blocks. */
public final class AdminSubmissionStats {
  private AdminSubmissionStats() {}

  public record Totals(long all, long current, long previous) {}

  public record Summary(
      long value, BigDecimal deltaPercent, String deltaDirection, List<Long> sparklineSeries) {
    public Summary {
      sparklineSeries = List.copyOf(sparklineSeries);
    }
  }

  public record DayCount(LocalDate day, long count) {}

  public record LanguageDay(LocalDate day, String language, long count) {}

  public record LanguageSeries(String languageCode, List<Long> values) {
    public LanguageSeries {
      values = List.copyOf(values);
    }
  }

  public record Languages(List<String> pointLabels, List<LanguageSeries> series) {
    public Languages {
      pointLabels = List.copyOf(pointLabels);
      series = List.copyOf(series);
    }
  }

  public record VerdictSlice(String verdict, long count, int percent) {}

  public record Verdicts(List<VerdictSlice> slices) {
    public Verdicts {
      slices = List.copyOf(slices);
    }
  }

  /** Identity supplies the published, non-deleted problem sets in catalogue sort order. */
  public record LevelProblems(UUID levelId, Set<UUID> problemIds) {
    public LevelProblems {
      Objects.requireNonNull(levelId);
      problemIds = Set.copyOf(problemIds);
    }
  }

  public record ProblemCount(UUID problemId, long submissionCount, long acceptedCount) {}

  public record LevelCount(UUID levelId, long submissions, long accepted) {}

  public record Difficulty(List<LevelCount> groups) {
    public Difficulty {
      groups = List.copyOf(groups);
    }
  }

  public record Days(List<DayCount> columns, long weekTotal, long dailyAverage) {
    public Days {
      columns = List.copyOf(columns);
    }
  }

  public record MonthCount(YearMonth month, long count) {}

  public record Months(List<MonthCount> points, long monthTotal) {
    public Months {
      points = List.copyOf(points);
    }
  }

  public record Top(List<ProblemCount> items) {
    public Top {
      items = List.copyOf(items);
    }
  }
}
