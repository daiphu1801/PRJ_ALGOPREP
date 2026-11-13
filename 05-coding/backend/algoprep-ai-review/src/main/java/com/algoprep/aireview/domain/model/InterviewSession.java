package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Row of {@code ai.interview_sessions}. The nullable F5-28 settings are {@code null} for a
 * SUBMISSION entry (BD 1.4); {@code topics} is empty unless the entry is TOPIC_SELECTION.
 */
public record InterviewSession(
    UUID id,
    UUID userId,
    InterviewEntryType entryType,
    UUID submissionId,
    String questionRef,
    String questionText,
    List<String> topics,
    InterviewerLevel interviewerLevel,
    Integer maxTurns,
    Boolean hintAllowed,
    int hintCount,
    InterviewSessionStage stage,
    int turnCount,
    Instant startedAt,
    Instant endedAt,
    Instant updatedAt) {
  public InterviewSession {
    topics = topics == null ? List.of() : List.copyOf(topics);
  }

  public boolean owns(UUID callerId) {
    return userId.equals(callerId);
  }

  /** Turn budget spent: {@code turn_count >= max_turns} when the session has a limit. */
  public boolean outOfTurns() {
    return maxTurns != null && turnCount >= maxTurns;
  }

  public boolean hintsEnabled() {
    return Boolean.TRUE.equals(hintAllowed);
  }
}
