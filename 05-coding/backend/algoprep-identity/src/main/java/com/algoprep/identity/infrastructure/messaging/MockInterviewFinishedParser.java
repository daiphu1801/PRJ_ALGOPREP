package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.model.FinishedMockInterview;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the Outbox envelope of DD api F5 section 33. A body that fails here can never succeed on a
 * retry, so the caller drops it instead of requeueing. The score and the rubric are not read.
 */
final class MockInterviewFinishedParser {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private MockInterviewFinishedParser() {}

  static FinishedMockInterview parse(byte[] body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      if (!"MockInterviewFinished".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      JsonNode p = root.path("payload");
      return new FinishedMockInterview(
          UUID.fromString(root.path("eventId").asString("")),
          UUID.fromString(p.path("userId").asString("")),
          UUID.fromString(p.path("sessionId").asString("")),
          Instant.parse(root.path("occurredAt").asString("")));
    } catch (RuntimeException e) {
      // The message names the failed check only; the body may hold ids and is never logged.
      throw new IllegalArgumentException("invalid MockInterviewFinished envelope", e);
    }
  }
}
