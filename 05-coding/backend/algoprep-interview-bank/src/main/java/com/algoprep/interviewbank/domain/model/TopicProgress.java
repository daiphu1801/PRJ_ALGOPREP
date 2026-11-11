package com.algoprep.interviewbank.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One topic row of the caller's progress. {@code completionPercent} is derived on read (BR-IB-09):
 * zero when the topic has no ACTIVE question, otherwise clamped to 100 as a safety net.
 */
public record TopicProgress(
    UUID topicId,
    String topicCode,
    String topicName,
    int sortOrder,
    int questionsAttempted,
    int totalQuestions,
    int completionPercent,
    Instant lastPracticedAt) {

  /** Zero without ACTIVE questions, otherwise rounded half up and clamped to 100. */
  public static int percent(int questionsAttempted, int totalQuestions) {
    return totalQuestions <= 0
        ? 0
        : (int) Math.min(100L, Math.round(questionsAttempted * 100.0 / totalQuestions));
  }
}
