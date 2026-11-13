package com.algoprep.aireview.domain.model;

import java.util.Objects;

/**
 * One entry of the short-term interview memory (BR-AI-10). The first entries are the session
 * context blocks (data, never instructions), then the USER and AI turns in order. There is no
 * system kind: the system instruction is rebuilt on every call from the ACTIVE template.
 */
public record ChatMessage(Kind kind, String content) {
  /** Kind of entry. */
  public enum Kind {
    PROBLEM,
    USER_CODE,
    QUESTION,
    TOPICS,
    USER,
    AI;

    public boolean isContext() {
      return this != USER && this != AI;
    }
  }

  public ChatMessage {
    Objects.requireNonNull(kind, "kind");
    Objects.requireNonNull(content, "content");
  }
}
