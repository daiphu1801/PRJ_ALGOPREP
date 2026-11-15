package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Minimal list projection; source, testcase and compile messages are never loaded. */
public record LearnerSubmissionCandidate(
    UUID submissionId,
    UUID problemId,
    String language,
    String status,
    Integer passedCount,
    Integer totalCount,
    Integer runtimeMs,
    Integer memoryKb,
    Instant submittedAt) {
  public Cursor cursor() {
    return new Cursor(submittedAt, submissionId);
  }

  public record Cursor(Instant submittedAt, UUID submissionId) {}
}
