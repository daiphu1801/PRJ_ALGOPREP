package com.algoprep.judge.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record SubmissionFinishedProgress(
    UUID submissionId,
    Instant occurredAt,
    String status,
    Integer passedCount,
    Integer totalCount,
    BigDecimal scoreRatio,
    Integer firstFailingOrder,
    Long runtimeMs,
    Long memoryKb,
    Instant finishedAt,
    @JsonIgnore Integer judgedTestcaseCount)
    implements SubmissionProgress {
  public SubmissionFinishedProgress {
    Objects.requireNonNull(submissionId);
    Objects.requireNonNull(occurredAt);
    Objects.requireNonNull(finishedAt);
    if (status == null
        || !Set.of(
                "ACCEPTED",
                "WRONG_ANSWER",
                "TIME_LIMIT_EXCEEDED",
                "RUNTIME_ERROR",
                "COMPILE_ERROR",
                "SYSTEM_ERROR")
            .contains(status)
        || (judgedTestcaseCount != null
            && (judgedTestcaseCount < 1 || judgedTestcaseCount > Integer.MAX_VALUE - 3))) {
      throw new IllegalArgumentException("Invalid final submission progress");
    }
    if ("COMPILE_ERROR".equals(status) || "SYSTEM_ERROR".equals(status)) {
      passedCount = null;
      totalCount = null;
      scoreRatio = null;
    }
  }

  @Override
  public String type() {
    return "SUBMISSION_FINISHED";
  }

  @Override
  public int seq() {
    return judgedTestcaseCount == null ? 3 : 3 + judgedTestcaseCount;
  }

  @Override
  public boolean isFinal() {
    return true;
  }
}
