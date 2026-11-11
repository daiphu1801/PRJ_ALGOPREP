package com.algoprep.aireview.application.dto;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Cross-module contract for the F6 practice feedback (DD F6 sections 25 and 26). The input type has
 * no way to hand over free system text beyond the fixed instruction: the learner answer and the
 * question text only ever travel as data blocks.
 */
public final class PracticeFeedback {
  /** Hard cap on one LLM call (DD F6 section 25). */
  public static final Duration TOTAL_TIMEOUT = Duration.ofSeconds(30);

  private PracticeFeedback() {}

  /** One rubric row of the snapshot; {@code description} may be null. */
  public record Criterion(String criterionCode, BigDecimal weightPercent, String description) {
    public Criterion {
      Objects.requireNonNull(criterionCode, "criterionCode");
      Objects.requireNonNull(weightPercent, "weightPercent");
    }
  }

  /**
   * {@code callId} is the ledger idempotency key and {@code answerId} the ledger owner id, both
   * made by the caller before any gate. {@code modelId} may be blank: the adapter then uses its
   * default.
   */
  public record Input(
      UUID callId,
      UUID userId,
      UUID answerId,
      String systemInstruction,
      String modelId,
      List<Criterion> rubric,
      String questionTitle,
      String questionContent,
      String answerText) {
    public Input {
      Objects.requireNonNull(callId, "callId");
      Objects.requireNonNull(userId, "userId");
      Objects.requireNonNull(answerId, "answerId");
      Objects.requireNonNull(systemInstruction, "systemInstruction");
      Objects.requireNonNull(modelId, "modelId");
      Objects.requireNonNull(questionTitle, "questionTitle");
      Objects.requireNonNull(questionContent, "questionContent");
      Objects.requireNonNull(answerText, "answerText");
      rubric = List.copyOf(Objects.requireNonNull(rubric, "rubric"));
      if (rubric.isEmpty()) {
        throw new IllegalArgumentException("rubric must not be empty");
      }
    }

    @Override
    public String toString() {
      return "PracticeFeedback.Input[call=" + callId + ", answer=" + answerId + "]";
    }
  }

  /** What the Lua gate did; {@code retryAfterSeconds} is only set for QUOTA_EXCEEDED. */
  public enum Status {
    GRANTED,
    BYPASSED,
    QUOTA_EXCEEDED
  }

  /** Opaque receipt of one reservation; handing it back refunds it exactly once. */
  public record Ticket(
      Status status, UUID userId, String requestId, long epochHour, long retryAfterSeconds) {
    public boolean charged() {
      return status == Status.GRANTED;
    }
  }

  /** One scored criterion; the code is already the exact string of the snapshot. */
  public record Scored(String criterionCode, int achievementPercent) {}

  /** A feedback that already passed the schema check against the snapshot. */
  public record Output(
      List<String> achievedPoints,
      List<String> missingPoints,
      List<String> improvementSuggestions,
      List<Scored> criteria,
      String modelName) {
    public Output {
      achievedPoints = List.copyOf(achievedPoints);
      missingPoints = List.copyOf(missingPoints);
      improvementSuggestions = List.copyOf(improvementSuggestions);
      criteria = List.copyOf(criteria);
    }

    @Override
    public String toString() {
      return "PracticeFeedback.Output[redacted]";
    }
  }
}
