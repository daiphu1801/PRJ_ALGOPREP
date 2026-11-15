package com.algoprep.identity.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * BR-10 and BR-12 on top of the judge figures of one class (DD logic F1). Pure functions: the
 * caller supplies the moment of the read, so every rule is testable at a fixed clock.
 */
public final class ClassProgressRules {
  /** Completed ISO weeks asked from the judge; #57 draws exactly this many points. */
  public static final int WEEKS = 6;

  /** The share of assigned problems below which a student is on the watch list. */
  private static final double WATCH_BELOW = 0.5;

  private static final Duration ABSENT_AFTER = Duration.ofDays(7);
  private static final ZoneOffset LOCAL = ZoneOffset.ofHours(7);
  private static final int ATTENTION_LIMIT = 5;

  public static final String REASON_ABSENT_DAYS = "ABSENT_DAYS";
  public static final String REASON_SCORE_DROP_WEEKS = "SCORE_DROP_WEEKS";
  public static final String REASON_LOW_COMPLETION = "LOW_COMPLETION_PERCENT";

  private ClassProgressRules() {}

  /** The row of one student; the completion is a fraction, null when nothing is assigned. */
  public static ClassStudentRow row(
      UUID classId, String className, ClassRosterEntry student, StudentMetrics m, Instant now) {
    Double completion = completion(m);
    return new ClassStudentRow(
        classId, className, student, m, status(m, completion, now), completion, avgScore(m));
  }

  /** Accepted share of the problems still assigned; null when none is assigned (BR-12). */
  public static Double completion(StudentMetrics m) {
    if (m.assignments().isEmpty()) {
      return null;
    }
    long accepted =
        m.assignments().stream().filter(StudentMetrics.AssignmentOutcome::accepted).count();
    return (double) accepted / m.assignments().size();
  }

  /** The judge accepted ratio on the 10 point scale, one decimal; null without a submission. */
  public static Double avgScore(StudentMetrics m) {
    return m.acceptedRatio() == null ? null : round1(m.acceptedRatio() * 10);
  }

  public static ClassStudentStatus status(StudentMetrics m, Double completion, Instant now) {
    if (m.lastSubmittedAt() == null) {
      return ClassStudentStatus.INSUFFICIENT_DATA;
    }
    if (m.lastSubmittedAt().isBefore(now.minus(ABSENT_AFTER))) {
      return ClassStudentStatus.ABSENT;
    }
    if (decline(m.weeks()) != null) {
      return ClassStudentStatus.NEEDS_SUPPORT;
    }
    if (completion != null && completion < WATCH_BELOW) {
      return ClassStudentStatus.WATCH;
    }
    return ClassStudentStatus.ON_TRACK;
  }

  /** A run of falling accepted ratios ending at the latest completed week. */
  public record Decline(int weeks, double points) {}

  /**
   * The run of strictly falling ratios that ends at the latest week; null when it is shorter than
   * two weeks or a week without an eligible submission breaks it. {@code points} is the fall from
   * the first to the last week of the run, in percentage points.
   */
  public static Decline decline(List<StudentMetrics.WeekPoint> weeks) {
    int last = weeks.size() - 1;
    if (last < 1 || weeks.get(last).acceptedRatio() == null) {
      return null;
    }
    int first = last;
    while (first > 0
        && weeks.get(first - 1).acceptedRatio() != null
        && weeks.get(first - 1).acceptedRatio() > weeks.get(first).acceptedRatio()) {
      first--;
    }
    int run = last - first + 1;
    if (run < 2) {
      return null;
    }
    return new Decline(
        run, (weeks.get(first).acceptedRatio() - weeks.get(last).acceptedRatio()) * 100);
  }

  /** Mean of the completion of the rows that have one, whole percent; null when none has. */
  public static Integer meanCompletionPercent(List<ClassStudentRow> rows) {
    return rows.stream()
        .map(ClassStudentRow::completion)
        .filter(c -> c != null)
        .mapToDouble(Double::doubleValue)
        .average()
        .stream()
        .mapToObj(avg -> (int) Math.round(avg * 100))
        .findFirst()
        .orElse(null);
  }

  /** Mean of the 10 point scores of the rows that have one, one decimal; null when none has. */
  public static Double meanScore(List<ClassStudentRow> rows) {
    return rows.stream()
        .map(ClassStudentRow::avgScore)
        .filter(s -> s != null)
        .mapToDouble(Double::doubleValue)
        .average()
        .stream()
        .mapToObj(ClassProgressRules::round1)
        .findFirst()
        .orElse(null);
  }

  /**
   * One score per completed week for the given rows: the mean over the students who have an
   * eligible submission that week, null for a week nobody has one (never a 0).
   */
  public static List<Double> weeklyScores(List<ClassStudentRow> rows) {
    List<Double> scores = new ArrayList<>(WEEKS);
    for (int week = 0; week < WEEKS; week++) {
      int index = week;
      scores.add(
          rows.stream()
              .filter(r -> r.metrics() != null && r.metrics().weeks().size() > index)
              .map(r -> r.metrics().weeks().get(index).acceptedRatio())
              .filter(ratio -> ratio != null)
              .mapToDouble(Double::doubleValue)
              .average()
              .stream()
              .mapToObj(avg -> round1(avg * 10))
              .findFirst()
              .orElse(null));
    }
    return scores;
  }

  /** Monday of each of the last {@link #WEEKS} completed ISO weeks at {@code now}, oldest first. */
  public static List<LocalDate> weekStarts(Instant now) {
    LocalDate current =
        now.atOffset(LOCAL).toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    List<LocalDate> starts = new ArrayList<>(WEEKS);
    for (int back = WEEKS; back > 0; back--) {
      starts.add(current.minusWeeks(back));
    }
    return starts;
  }

  /**
   * The students who need attention, most serious first (DD api F1 section 0.8.2, owner
   * 2026-10-27): ABSENT by days since the last submission (more first), NEEDS_SUPPORT by the fall
   * of the accepted ratio (bigger first), WATCH by completion (lower first), then student id.
   */
  public static List<ClassMetricViews.AttentionStudent> attention(
      List<ClassStudentRow> rows, Instant now) {
    return rows.stream()
        .filter(r -> rank(r.status()) >= 0)
        .map(r -> new Ranked(r, severity(r, now)))
        .sorted(
            Comparator.comparingInt((Ranked r) -> rank(r.row().status()))
                .thenComparing(Ranked::severity, Comparator.reverseOrder())
                .thenComparing(r -> r.row().student().studentId())
                .thenComparing(r -> r.row().classId()))
        .limit(ATTENTION_LIMIT)
        .map(r -> toAttention(r.row(), now))
        .collect(Collectors.toList());
  }

  private record Ranked(ClassStudentRow row, double severity) {}

  private static int rank(ClassStudentStatus status) {
    return switch (status) {
      case ABSENT -> 0;
      case NEEDS_SUPPORT -> 1;
      case WATCH -> 2;
      default -> -1;
    };
  }

  private static double severity(ClassStudentRow r, Instant now) {
    return switch (r.status()) {
      case ABSENT -> absentDays(r.metrics(), now);
      case NEEDS_SUPPORT -> decline(r.metrics().weeks()).points();
      default -> 1 - r.completion();
    };
  }

  private static ClassMetricViews.AttentionStudent toAttention(ClassStudentRow r, Instant now) {
    String code;
    int value;
    switch (r.status()) {
      case ABSENT -> {
        code = REASON_ABSENT_DAYS;
        value = absentDays(r.metrics(), now);
      }
      case NEEDS_SUPPORT -> {
        code = REASON_SCORE_DROP_WEEKS;
        value = decline(r.metrics().weeks()).weeks();
      }
      default -> {
        code = REASON_LOW_COMPLETION;
        value = r.completionPercent();
      }
    }
    return new ClassMetricViews.AttentionStudent(
        r.classId(), r.student().studentId(), r.student().displayName(), r.status(), code, value);
  }

  /** Whole days from the last submission to {@code now}. */
  private static int absentDays(StudentMetrics m, Instant now) {
    return (int) Duration.between(m.lastSubmittedAt(), now).toDays();
  }

  private static double round1(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
