package com.algoprep.identity.domain.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Result shapes of the class metric reads #29 and #56 to #59 (DD api F1 sections 0.8.2, 0.8.3). */
public final class ClassMetricViews {
  private ClassMetricViews() {}

  /** One class card of #56; {@code completionPercent} and {@code avgScore} may be null. */
  public record ClassCard(
      InstructorClass owned, Integer completionPercent, Double avgScore, int absentCount) {}

  /** #56: {@code studentCount} counts people once, {@code absentStudentCount} too. */
  public record ClassesSummary(
      int classCount,
      int studentCount,
      Integer avgCompletionPercent,
      int absentStudentCount,
      List<ClassCard> items) {
    public ClassesSummary {
      items = List.copyOf(items);
    }
  }

  public record TrendPoint(LocalDate weekStart, Double avgScore) {}

  /** #57: six completed ISO weeks, one line; the class is null when all classes are summed. */
  public record ScoreTrend(UUID classId, String className, List<TrendPoint> points) {
    public ScoreTrend {
      points = List.copyOf(points);
    }
  }

  /** One line of #58; {@code reasonCode} and {@code reasonValue} let the client word the reason. */
  public record AttentionStudent(
      UUID classId,
      UUID studentId,
      String displayName,
      ClassStudentStatus status,
      String reasonCode,
      int reasonValue) {}

  /** #59: one page of (class, student) rows. */
  public record StudentPage(
      List<ClassStudentRow> items, int page, int pageSize, long totalCount, int totalPages) {
    public StudentPage {
      items = List.copyOf(items);
    }
  }

  /** #29: completion and the head count of each of the five labels. */
  public record ClassProgress(
      UUID classId,
      String className,
      int studentCount,
      Integer completionPercent,
      Double avgScore,
      Map<ClassStudentStatus, Integer> statusCounts) {
    public ClassProgress {
      statusCounts = Map.copyOf(statusCounts);
    }
  }
}
