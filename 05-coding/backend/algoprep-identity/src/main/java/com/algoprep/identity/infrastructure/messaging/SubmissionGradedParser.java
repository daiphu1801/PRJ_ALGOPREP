package com.algoprep.identity.infrastructure.messaging;

import com.algoprep.identity.domain.model.GradedSubmission;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the envelope of DD api F4 section 25.2 and checks it. A body that fails here can never
 * succeed on a retry, so the caller drops it instead of requeueing.
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
  private static final Set<String> LANGUAGES = Set.of("JAVA", "CPP", "PYTHON");

  private SubmissionGradedParser() {}

  static GradedSubmission parse(byte[] body) {
    try {
      JsonNode root = MAPPER.readTree(body);
      if (!"SubmissionGraded".equals(root.path("eventType").asString(""))) {
        throw new IllegalArgumentException("unexpected event type");
      }
      JsonNode p = root.path("payload");
      String verdict = p.path("verdict").asString("");
      String language = p.path("language").asString("");
      if (!VERDICTS.contains(verdict) || !LANGUAGES.contains(language)) {
        throw new IllegalArgumentException("unknown verdict or language");
      }
      JsonNode ratio = p.path("passedTestcaseRatio");
      BigDecimal value = ratio.isNull() || ratio.isMissingNode() ? null : ratio.decimalValue();
      if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
        throw new IllegalArgumentException("ratio out of range");
      }
      return new GradedSubmission(
          UUID.fromString(root.path("eventId").asString("")),
          UUID.fromString(p.path("userId").asString("")),
          UUID.fromString(p.path("problemId").asString("")),
          verdict,
          value,
          language,
          Instant.parse(p.path("submittedAt").asString("")),
          p.path("firstAttemptForProblem").asBoolean(false));
    } catch (RuntimeException e) {
      // The message names the failed check only; the body may hold ids and is never logged.
      throw new IllegalArgumentException("invalid SubmissionGraded envelope", e);
    }
  }
}
