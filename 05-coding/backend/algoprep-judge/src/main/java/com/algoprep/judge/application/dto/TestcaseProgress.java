package com.algoprep.judge.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record TestcaseProgress(
    UUID submissionId,
    Instant occurredAt,
    int order,
    String verdict,
    int completedCount,
    int passedCount,
    int totalCount)
    implements SubmissionProgress {
  public TestcaseProgress {
    Objects.requireNonNull(submissionId);
    Objects.requireNonNull(occurredAt);
    if (verdict == null
        || !Set.of("AC", "WA", "TLE", "MLE", "RE").contains(verdict)
        || order < 1
        || order != completedCount
        || totalCount < completedCount
        || passedCount < 0
        || passedCount > completedCount
        || totalCount > Integer.MAX_VALUE - 3) {
      throw new IllegalArgumentException("Invalid testcase progress");
    }
  }

  @Override
  public String type() {
    return "TESTCASE_FINISHED";
  }

  @Override
  public int seq() {
    return 2 + order;
  }

  @Override
  public boolean isFinal() {
    return false;
  }
}
