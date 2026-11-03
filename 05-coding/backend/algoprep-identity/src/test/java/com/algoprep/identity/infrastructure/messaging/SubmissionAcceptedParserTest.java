package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.AcceptedSubmission;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The envelope of DD api F4 sections 25.1 and 25.3, as the judge relay will publish it. */
class SubmissionAcceptedParserTest {
  private static final String EVENT = "11111111-1111-1111-1111-111111111111";
  private static final String USER = "22222222-2222-2222-2222-222222222222";
  private static final String PROBLEM = "33333333-3333-3333-3333-333333333333";
  private static final String SUBMISSION = "44444444-4444-4444-4444-444444444444";

  private static byte[] body(String type, String occurredAt, String user) {
    return ("{\"eventId\":\""
            + EVENT
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"schemaVersion\":1,\"payload\":{\"submissionId\":\""
            + SUBMISSION
            + "\",\"userId\":\""
            + user
            + "\",\"problemId\":\""
            + PROBLEM
            + "\",\"language\":\"JAVA\"}}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void readsTheFieldsTheFeedNeedsAndTakesTheTimeFromTheEnvelope() {
    AcceptedSubmission s =
        SubmissionAcceptedParser.parse(body("SubmissionAccepted", "2026-11-03T10:00:00Z", USER));

    assertThat(s.eventId()).isEqualTo(UUID.fromString(EVENT));
    assertThat(s.userId()).isEqualTo(UUID.fromString(USER));
    assertThat(s.problemId()).isEqualTo(UUID.fromString(PROBLEM));
    assertThat(s.submissionId()).isEqualTo(UUID.fromString(SUBMISSION));
    assertThat(s.occurredAt()).isEqualTo(Instant.parse("2026-11-03T10:00:00Z"));
  }

  @Test
  void rejectsWhatCanNeverSucceedOnARetry() {
    assertThatThrownBy(
            () ->
                SubmissionAcceptedParser.parse(
                    body("SubmissionGraded", "2026-11-03T10:00:00Z", USER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> SubmissionAcceptedParser.parse(body("SubmissionAccepted", "yesterday", USER)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SubmissionAcceptedParser.parse(
                    body("SubmissionAccepted", "2026-11-03T10:00:00Z", "nope")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> SubmissionAcceptedParser.parse("not json".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
