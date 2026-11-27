package com.algoprep.judge.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record SubmissionStatusProgress(
    UUID submissionId, Instant occurredAt, String status, Integer totalCount)
    implements SubmissionProgress {
  public SubmissionStatusProgress {
    Objects.requireNonNull(submissionId);
    Objects.requireNonNull(occurredAt);
    if (!("COMPILING".equals(status) && totalCount == null)
        && !("JUDGING".equals(status) && totalCount != null && totalCount > 0)) {
      throw new IllegalArgumentException("Invalid submission progress");
    }
  }

  @Override
  public String type() {
    return "STATUS_CHANGED";
  }

  @Override
  public int seq() {
    return "COMPILING".equals(status) ? 1 : 2;
  }

  @Override
  public boolean isFinal() {
    return false;
  }
}
