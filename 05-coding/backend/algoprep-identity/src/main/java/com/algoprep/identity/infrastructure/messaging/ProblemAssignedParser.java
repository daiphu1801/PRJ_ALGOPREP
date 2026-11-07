package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.model.ClassProblemAssignment;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the Outbox envelope of DD api F2 section 53. A body that fails here can never succeed on a
 * retry, so the caller drops it instead of requeueing.
 */
final class ProblemAssignedParser {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private ProblemAssignedParser() {}

  static ClassProblemAssignment parse(byte[] body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      if (!"ProblemAssignedToClass".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      JsonNode p = root.path("payload");
      return new ClassProblemAssignment(
          UUID.fromString(root.path("eventId").asString("")),
          UUID.fromString(p.path("classId").asString("")),
          UUID.fromString(p.path("problemId").asString("")),
          UUID.fromString(p.path("actorUserId").asString("")),
          Instant.parse(root.path("occurredAt").asString("")));
    } catch (RuntimeException e) {
      // The message names the failed check only; the body may hold ids and is never logged.
      throw new IllegalArgumentException("invalid ProblemAssignedToClass envelope", e);
    }
  }
}
