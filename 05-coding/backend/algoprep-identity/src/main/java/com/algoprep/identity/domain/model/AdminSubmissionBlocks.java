package com.algoprep.identity.domain.model;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Identity's own shapes for the seven judge-backed blocks of the admin overview (DD api F1 section
 * 0.8.5, #43 and #67 to #72). The ports translate the judge and problem-bank answers into these.
 */
public final class AdminSubmissionBlocks {
  private AdminSubmissionBlocks() {}

  public record LanguageSeries(String languageCode, List<Long> values) {
    public LanguageSeries {
      values = List.copyOf(values);
    }
  }

  /** #67: twenty labelled points and one series per submission language. */
  public record Languages(List<String> pointLabels, List<LanguageSeries> series) {
    public Languages {
      pointLabels = List.copyOf(pointLabels);
      series = List.copyOf(series);
    }
  }

  public record VerdictSlice(String verdict, long count, int percent) {}

  /** #68: the five final verdicts, percentages already rounded by the judge. */
  public record Verdicts(List<VerdictSlice> slices) {
    public Verdicts {
      slices = List.copyOf(slices);
    }
  }

  /** The published problems of one level, the scope the judge counts submissions in. */
  public record LevelScope(UUID levelId, Set<UUID> problemIds) {
    public LevelScope {
      problemIds = Set.copyOf(problemIds);
    }
  }

  /** What the judge counted for one level. */
  public record LevelCount(UUID levelId, long submissions, long accepted) {}

  public record DifficultyGroup(
      UUID levelId, String levelCode, String levelDisplayName, long submissions, long accepted) {}

  /** #69: one group per level in catalogue order, and the keys of the two series. */
  public record Difficulty(List<DifficultyGroup> groups, List<String> legend) {
    public Difficulty {
      groups = List.copyOf(groups);
      legend = List.copyOf(legend);
    }
  }

  public record DayColumn(String day, long count) {}

  /** #70: exactly seven days, oldest first. */
  public record Days(List<DayColumn> columns, long weekTotal, long dailyAverage) {
    public Days {
      columns = List.copyOf(columns);
    }
  }

  public record MonthPoint(String month, long count) {}

  /** #71: exactly six months as {@code yyyy-MM}, oldest first. */
  public record Months(List<MonthPoint> points, long monthTotal) {
    public Months {
      points = List.copyOf(points);
    }
  }

  /** What the judge counted for one problem. */
  public record ProblemCount(UUID problemId, long submissionCount) {}

  public record TopProblem(UUID problemId, String title, long submissionCount) {}

  /** #72: at most four published problems with the most submissions. */
  public record TopProblems(List<TopProblem> items) {
    public TopProblems {
      items = List.copyOf(items);
    }
  }

  /** A level of the problem catalogue in {@code sort_order}. */
  public record LevelRef(UUID id, String code, String displayName) {}

  /** A published, non-deleted problem. */
  public record PublishedProblem(UUID id, String title, UUID levelId) {}
}
