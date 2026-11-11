package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Row of {@code ai.interview_turns}; {@code turnIndex} counts every line from 1. */
public record InterviewTurn(
    UUID id,
    UUID sessionId,
    int turnIndex,
    Role role,
    InterviewStage stage,
    String content,
    Instant createdAt) {
  /** Who wrote the line. */
  public enum Role {
    USER,
    AI
  }
}
