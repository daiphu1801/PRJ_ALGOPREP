package com.algoprep.problembank.infrastructure.messaging;

import com.algoprep.problembank.domain.model.ProblemStatsModels.GradedSubmission;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the envelope of DD api F4 section 25.2 and keeps what BR-20 counts. A body that fails here
 * can never succeed on a retry, so the caller drops it instead of requeueing.
 */
final class SubmissionGradedParser {
  private static final JsonMapper MAPPER = JsonMapper.builder().build();
  private static final Set<String> VERDICTS =
      Set.of(
          "ACCEPTED",
          "WRONG_ANSWER",
          "TIME_LIMIT_EXCEEDED",
          "RUNTIME_ERROR",
          "COMPILE_ERROR",
          "SYSTEM_ERROR");

  private SubmissionGradedParser() {}

  static GradedSubmission parse(byte[] body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      if (!"SubmissionGraded".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      JsonNode p = root.path("payload");
      String verdict = p.path("verdict").asString("");
      if (!VERDICTS.contains(verdict)) {
        throw new IllegalArgumentException("unknown verdict");
      }
      return new GradedSubmission(
          UUID.fromString(root.path("eventId").asString("")),
          UUID.fromString(p.path("problemId").asString("")),
          verdict);
    } catch (RuntimeException e) {
      // The message names the failed check only; the body may hold ids and is never logged.
      throw new IllegalArgumentException("invalid SubmissionGraded envelope", e);
    }
  }
}
