package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.FinishedMockInterview;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The Outbox envelope of DD api F5 section 33, as the ai-review relay publishes it. */
class MockInterviewFinishedParserTest {
  private static final String EVENT = "11111111-1111-1111-1111-111111111111";
  private static final String USER = "22222222-2222-2222-2222-222222222222";
  private static final String SESSION = "33333333-3333-3333-3333-333333333333";

  private static byte[] body(String type, String occurredAt, String user) {
    return ("{\"eventId\":\""
            + EVENT
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"schemaVersion\":1,\"payload\":{\"sessionId\":\""
            + SESSION
            + "\",\"userId\":\""
            + user
            + "\",\"entryType\":\"TOPIC_SELECTION\",\"submissionId\":null,\"overallScore\":6.55,"
            + "\"rubric\":{\"CLARITY\":8,\"TECHNICAL_ACCURACY\":6,\"PUSHBACK_HANDLING\":7,"
            + "\"COMPLEXITY_AWARENESS\":5}}}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void readsTheFieldsTheFeedNeedsAndTakesTheTimeFromTheEnvelope() {
    FinishedMockInterview r =
        MockInterviewFinishedParser.parse(
            body("MockInterviewFinished", "2026-11-10T08:00:00Z", USER));

    assertThat(r.eventId()).isEqualTo(UUID.fromString(EVENT));
    assertThat(r.userId()).isEqualTo(UUID.fromString(USER));
    assertThat(r.sessionId()).isEqualTo(UUID.fromString(SESSION));
    assertThat(r.occurredAt()).isEqualTo(Instant.parse("2026-11-10T08:00:00Z"));
  }

  @Test
  void rejectsWhatCanNeverSucceedOnARetry() {
    assertThatThrownBy(
            () ->
                MockInterviewFinishedParser.parse(
                    body("SolutionReviewCompleted", "2026-11-10T08:00:00Z", USER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                MockInterviewFinishedParser.parse(body("MockInterviewFinished", "yesterday", USER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                MockInterviewFinishedParser.parse(
                    body("MockInterviewFinished", "2026-11-10T08:00:00Z", "not-a-uuid")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> MockInterviewFinishedParser.parse("[]".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
