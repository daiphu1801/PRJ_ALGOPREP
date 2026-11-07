package com.algoprep.problembank.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Models of the {@code problem_stats} read model (DD F2 sections 41 and 59.2, BR-20). */
public final class ProblemStatsModels {
  private ProblemStatsModels() {}

  /** The part of a {@code SubmissionGraded} event that BR-20 counts. */
  public record GradedSubmission(UUID eventId, UUID problemId, String verdict) {}

  /**
   * One problem with its counters; {@code acRate} and {@code updatedAt} are null and the count is 0
   * while the problem has no {@code problem_stats} row.
   */
  public record ProblemStatsRow(
      UUID authorId, String status, int submissionCount, BigDecimal acRate, Instant updatedAt) {}
}
