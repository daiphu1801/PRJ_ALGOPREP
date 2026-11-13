package com.algoprep.judge.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Endpoint-specific metadata shapes, without extra identifiers or administrative fields. */
public final class LearnerSubmissionLists {
  private LearnerSubmissionLists() {}

  public record ProgressProblem(
      String code, String title, String slug, String levelDisplayName, boolean withdrawn) {}

  public record AcceptedProblem(String code, String title, String slug, String levelDisplayName) {}

  public record InProgressItem(
      UUID problemId,
      ProgressProblem problem,
      UUID latestSubmissionId,
      String language,
      String status,
      Integer passedCount,
      Integer totalCount,
      Instant submittedAt) {}

  public record AcceptedItem(
      UUID submissionId,
      UUID problemId,
      AcceptedProblem problem,
      String language,
      Integer runtimeMs,
      Integer memoryKb,
      Instant submittedAt) {}
}
