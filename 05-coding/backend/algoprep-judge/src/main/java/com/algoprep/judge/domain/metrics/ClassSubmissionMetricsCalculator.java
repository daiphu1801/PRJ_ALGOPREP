package com.algoprep.judge.domain.metrics;

import com.algoprep.judge.domain.model.ClassMetricSubmission;
import com.algoprep.judge.domain.model.ClassMetricsScope;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics.AssignmentResult;
import com.algoprep.judge.domain.model.StudentSubmissionMetrics.Week;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** BR-JD-13: local days, completed ISO weeks, and assignment-local best scores. */
public final class ClassSubmissionMetricsCalculator {
  private static final ZoneOffset ZONE = ZoneOffset.ofHours(7);
  private static final Set<String> ELIGIBLE =
      Set.of("ACCEPTED", "WRONG_ANSWER", "TIME_LIMIT_EXCEEDED", "RUNTIME_ERROR", "COMPILE_ERROR");
  private static final Comparator<ClassMetricSubmission> BEST =
      Comparator.comparing(ClassMetricSubmission::scoreRatio)
          .thenComparing(ClassMetricSubmission::submittedAt)
          .thenComparing(row -> row.id().toString());

  private ClassSubmissionMetricsCalculator() {}

  public static List<StudentSubmissionMetrics> calculate(
      ClassMetricsScope scope, List<ClassMetricSubmission> rows, Instant now) {
    var byStudent = rows.stream().collect(Collectors.groupingBy(ClassMetricSubmission::studentId));
    return scope.students().stream()
        .map(
            student ->
                metrics(
                    scope,
                    byStudent.getOrDefault(student.studentId(), List.of()),
                    student,
                    now.atOffset(ZONE).toLocalDate()))
        .toList();
  }

  public static LocalDate weekStart(Instant now) {
    return now.atOffset(ZONE)
        .toLocalDate()
        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
  }

  private static StudentSubmissionMetrics metrics(
      ClassMetricsScope scope,
      List<ClassMetricSubmission> rows,
      ClassMetricsScope.Student student,
      LocalDate today) {
    return new StudentSubmissionMetrics(
        student.studentId(),
        streak(rows, today),
        rows.stream().map(ClassMetricSubmission::submittedAt).max(Instant::compareTo).orElse(null),
        ratio(rows),
        weeks(rows, today, scope.weekCount()),
        scope.assignments().stream()
            .filter(assignment -> assignment.removedAt() == null)
            .map(assignment -> assignmentResult(assignment, rows))
            .toList());
  }

  private static int streak(List<ClassMetricSubmission> rows, LocalDate today) {
    var days =
        rows.stream()
            .map(row -> row.submittedAt().atOffset(ZONE).toLocalDate())
            .collect(Collectors.toSet());
    int result = 0;
    while (days.contains(today.minusDays(result))) {
      result++;
    }
    return result;
  }

  private static List<Week> weeks(List<ClassMetricSubmission> rows, LocalDate today, int count) {
    LocalDate current = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    List<Week> result = new ArrayList<>();
    for (int index = count; index > 0; index--) {
      LocalDate start = current.minusWeeks(index);
      var inWeek =
          rows.stream()
              .filter(
                  row -> {
                    LocalDate day = row.submittedAt().atOffset(ZONE).toLocalDate();
                    return !day.isBefore(start) && day.isBefore(start.plusWeeks(1));
                  })
              .toList();
      result.add(new Week(start, inWeek.size(), accepted(inWeek), ratio(inWeek)));
    }
    return List.copyOf(result);
  }

  private static AssignmentResult assignmentResult(
      ClassMetricsScope.Assignment assignment, List<ClassMetricSubmission> rows) {
    var relevant =
        rows.stream()
            .filter(row -> row.problemId().equals(assignment.problemId()))
            .filter(row -> !row.submittedAt().isBefore(assignment.assignedAt()))
            .toList();
    var best = relevant.stream().filter(row -> row.scoreRatio() != null).max(BEST).orElse(null);
    return new AssignmentResult(
        assignment.problemId(),
        assignment.assignedAt(),
        best == null ? null : best.scoreRatio(),
        best == null ? null : best.verdict(),
        accepted(relevant) > 0);
  }

  private static long accepted(List<ClassMetricSubmission> rows) {
    return rows.stream().filter(row -> "ACCEPTED".equals(row.verdict())).count();
  }

  private static Double ratio(List<ClassMetricSubmission> rows) {
    long eligible = rows.stream().filter(row -> ELIGIBLE.contains(row.verdict())).count();
    return eligible == 0 ? null : (double) accepted(rows) / eligible;
  }
}
