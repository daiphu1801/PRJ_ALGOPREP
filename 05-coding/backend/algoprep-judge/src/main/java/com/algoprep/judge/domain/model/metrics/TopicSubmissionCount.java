package com.algoprep.judge.domain.model;

import java.time.Instant;

/** lastSubmittedAt includes any attempt; only eligible terminal states enter the ratio. */
public record TopicSubmissionCount(
    long submissionCount, long acceptedCount, Instant lastSubmittedAt) {
  public Double acRate() {
    return submissionCount == 0 ? null : (double) acceptedCount / submissionCount;
  }
}
