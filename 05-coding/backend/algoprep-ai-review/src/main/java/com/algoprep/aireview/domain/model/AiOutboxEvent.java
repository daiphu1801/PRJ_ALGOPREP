package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One pending {@code ai.outbox_events} row. {@code payload} is the JSON text of the business
 * payload only; the envelope (event id, time, schema version) is built at publish time so the id
 * and the time survive every retry unchanged.
 */
public record AiOutboxEvent(
    UUID id, String eventType, String payload, Instant createdAt, int attempts) {

  public static final String SOLUTION_REVIEW_COMPLETED = "SolutionReviewCompleted";
  public static final String MOCK_INTERVIEW_FINISHED = "MockInterviewFinished";

  /** Routing key {@code context.aggregate.event} (DEC-2026-1106-f2-event-routing-keys). */
  public String routingKey() {
    return switch (eventType) {
      case SOLUTION_REVIEW_COMPLETED -> "ai.solution-review.completed";
      case MOCK_INTERVIEW_FINISHED -> "ai.interview-session.finished";
      default -> null;
    };
  }
}
