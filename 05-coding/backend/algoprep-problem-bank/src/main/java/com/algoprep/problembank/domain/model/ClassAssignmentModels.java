package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Read models of the instructor's class assignment screens (DD F2 sections 50 to 55). */
public final class ClassAssignmentModels {
  private ClassAssignmentModels() {}

  /** A class the caller is responsible for. */
  public record ClassRef(UUID id, String name) {}

  /**
   * The per-problem aggregate of the judge module; {@code acRatePercent} is null without a verdict.
   */
  public record Metric(long submissionCount, Double acRatePercent) {}

  /** One active (problem, class) pair with the problem's display data. */
  public record ActiveRow(
      UUID problemId,
      UUID classId,
      Instant assignedAt,
      String title,
      List<String> topicNames,
      String levelCode,
      String levelDisplayName,
      boolean withdrawn) {}

  public record Assignable(
      UUID problemId,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      List<String> topicNames) {}

  public record AssignablePage(List<Assignable> items, long totalItems, int page) {}

  /** What the assign step needs to know about a problem. */
  public record ProblemState(String title, boolean published) {}
}
