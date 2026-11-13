package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.CompletedSolutionReview;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The Outbox envelope of DD api F5 section 33, as the ai-review relay publishes it. */
class SolutionReviewCompletedParserTest {
  private static final String EVENT = "11111111-1111-1111-1111-111111111111";
  private static final String USER = "22222222-2222-2222-2222-222222222222";
  private static final String REVIEW = "33333333-3333-3333-3333-333333333333";

  private static byte[] body(String type, String occurredAt, String user) {
    return ("{\"eventId\":\""
            + EVENT
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"schemaVersion\":1,\"payload\":{\"reviewId\":\""
            + REVIEW
            + "\",\"submissionId\":\"44444444-4444-4444-4444-444444444444\",\"userId\":\""
            + user
            + "\",\"problemId\":\"55555555-5555-5555-5555-555555555555\",\"readabilityScore\":4}}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void readsTheFieldsTheFeedNeedsAndTakesTheTimeFromTheEnvelope() {
    CompletedSolutionReview r =
        SolutionReviewCompletedParser.parse(
            body("SolutionReviewCompleted", "2026-11-10T08:00:00Z", USER));

    assertThat(r.eventId()).isEqualTo(UUID.fromString(EVENT));
    assertThat(r.userId()).isEqualTo(UUID.fromString(USER));
    assertThat(r.reviewId()).isEqualTo(UUID.fromString(REVIEW));
    assertThat(r.occurredAt()).isEqualTo(Instant.parse("2026-11-10T08:00:00Z"));
  }

  @Test
  void rejectsWhatCanNeverSucceedOnARetry() {
    assertThatThrownBy(
            () ->
                SolutionReviewCompletedParser.parse(
                    body("MockInterviewFinished", "2026-11-10T08:00:00Z", USER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SolutionReviewCompletedParser.parse(
                    body("SolutionReviewCompleted", "yesterday", USER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SolutionReviewCompletedParser.parse(
                    body("SolutionReviewCompleted", "2026-11-10T08:00:00Z", "not-a-uuid")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> SolutionReviewCompletedParser.parse("[]".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
