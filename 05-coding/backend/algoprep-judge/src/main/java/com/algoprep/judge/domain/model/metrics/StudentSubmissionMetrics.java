package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record StudentSubmissionMetrics(
    UUID studentId,
    int streakDays,
    Instant lastSubmittedAt,
    Double acceptedRatio,
    List<Week> weeklyTrend,
    List<AssignmentResult> assignmentResults) {
  public StudentSubmissionMetrics {
    weeklyTrend = List.copyOf(weeklyTrend);
    assignmentResults = List.copyOf(assignmentResults);
  }

  public record Week(
      LocalDate weekStart, long submissionCount, long acceptedCount, Double acceptedRatio) {}

  public record AssignmentResult(
      UUID problemId,
      Instant assignedAt,
      BigDecimal bestRatio,
      String bestVerdict,
      boolean accepted) {}
}
