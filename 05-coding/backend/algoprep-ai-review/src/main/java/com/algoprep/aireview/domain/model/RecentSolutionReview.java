package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Row of the progress reads (#6, #7): the columns plus the result JSON to pick fields from. The
 * title is filled by the read handler for #7 only and stays null when problem-bank is unavailable.
 */
public record RecentSolutionReview(
    UUID reviewId,
    UUID submissionId,
    UUID problemId,
    String resultJson,
    Instant createdAt,
    String problemTitle) {
  public RecentSolutionReview(
      UUID reviewId, UUID submissionId, UUID problemId, String resultJson, Instant createdAt) {
    this(reviewId, submissionId, problemId, resultJson, createdAt, null);
  }

  public RecentSolutionReview withProblemTitle(String title) {
    return new RecentSolutionReview(
        reviewId, submissionId, problemId, resultJson, createdAt, title);
  }
}
