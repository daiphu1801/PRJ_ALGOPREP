package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.model.AcceptedSubmission;
import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the envelope of DD api F4 sections 25.1 and 25.3. A body that fails here can never succeed
 * on a retry, so the caller drops it instead of requeueing.
 */
final class SubmissionAcceptedParser {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private SubmissionAcceptedParser() {}

  static AcceptedSubmission parse(byte[] body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      if (!"SubmissionAccepted".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      JsonNode p = root.path("payload");
      return new AcceptedSubmission(
          UUID.fromString(root.path("eventId").asString("")),
          UUID.fromString(p.path("userId").asString("")),
          UUID.fromString(p.path("problemId").asString("")),
          UUID.fromString(p.path("submissionId").asString("")),
          Instant.parse(root.path("occurredAt").asString("")));
    } catch (RuntimeException e) {
      // The message names the failed check only; the body may hold ids and is never logged.
      throw new IllegalArgumentException("invalid SubmissionAccepted envelope", e);
    }
  }
}
