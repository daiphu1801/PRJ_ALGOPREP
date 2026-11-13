package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.model.CompletedSolutionReview;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the Outbox envelope of DD api F5 section 33. A body that fails here can never succeed on a
 * retry, so the caller drops it instead of requeueing.
 */
final class SolutionReviewCompletedParser {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private SolutionReviewCompletedParser() {}

  static CompletedSolutionReview parse(byte[] body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      if (!"SolutionReviewCompleted".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      JsonNode p = root.path("payload");
      return new CompletedSolutionReview(
          UUID.fromString(root.path("eventId").asString("")),
          UUID.fromString(p.path("userId").asString("")),
          UUID.fromString(p.path("reviewId").asString("")),
          Instant.parse(root.path("occurredAt").asString("")));
    } catch (RuntimeException e) {
      // The message names the failed check only; the body may hold ids and is never logged.
      throw new IllegalArgumentException("invalid SolutionReviewCompleted envelope", e);
    }
  }
}
