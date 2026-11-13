package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.GradedSubmission;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The envelope of DD api F4 section 25.2, as the judge relay will publish it. */
class SubmissionGradedParserTest {
  private static final String EVENT = "11111111-1111-1111-1111-111111111111";
  private static final String USER = "22222222-2222-2222-2222-222222222222";
  private static final String PROBLEM = "33333333-3333-3333-3333-333333333333";

  private static byte[] body(String type, String payload) {
    return ("{\"eventId\":\""
            + EVENT
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\"2026-11-03T05:00:01Z\",\"schemaVersion\":1,\"payload\":"
            + payload
            + "}")
        .getBytes(StandardCharsets.UTF_8);
  }

  private static String payload(String verdict, String ratio, String language) {
    return "{\"submissionId\":\"44444444-4444-4444-4444-444444444444\",\"userId\":\""
        + USER
        + "\",\"problemId\":\""
        + PROBLEM
        + "\",\"language\":\""
        + language
        + "\",\"verdict\":\""
        + verdict
        + "\",\"passedCount\":2,\"totalCount\":3,\"passedTestcaseRatio\":"
        + ratio
        + ",\"submittedAt\":\"2026-11-03T05:00:00Z\",\"firstAttemptForProblem\":true}";
  }

  @Test
  void readsEveryFieldTheReadModelsNeed() {
    GradedSubmission s =
        SubmissionGradedParser.parse(
            body("SubmissionGraded", payload("WRONG_ANSWER", "0.6667", "JAVA")));

    assertThat(s.eventId()).isEqualTo(UUID.fromString(EVENT));
    assertThat(s.userId()).isEqualTo(UUID.fromString(USER));
    assertThat(s.problemId()).isEqualTo(UUID.fromString(PROBLEM));
    assertThat(s.verdict()).isEqualTo("WRONG_ANSWER");
    assertThat(s.ratio()).isEqualByComparingTo(new BigDecimal("0.6667"));
    assertThat(s.language()).isEqualTo("JAVA");
    assertThat(s.submittedAt()).isEqualTo(Instant.parse("2026-11-03T05:00:00Z"));
    assertThat(s.firstAttemptForProblem()).isTrue();
  }

  @Test
  void aNullRatioStaysNull() {
    GradedSubmission s =
        SubmissionGradedParser.parse(
            body("SubmissionGraded", payload("COMPILE_ERROR", "null", "CPP")));

    assertThat(s.ratio()).isNull();
  }

  @Test
  void rejectsWhatCanNeverSucceedOnARetry() {
    assertThatThrownBy(
            () ->
                SubmissionGradedParser.parse(
                    body("SubmissionAccepted", payload("ACCEPTED", "1", "JAVA"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SubmissionGradedParser.parse(
                    body("SubmissionGraded", payload("PENDING", "0", "JAVA"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SubmissionGradedParser.parse(
                    body("SubmissionGraded", payload("ACCEPTED", "1", "RUST"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SubmissionGradedParser.parse(
                    body("SubmissionGraded", payload("ACCEPTED", "1.5", "JAVA"))))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () -> SubmissionGradedParser.parse("not json".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                SubmissionGradedParser.parse(
                    body(
                        "SubmissionGraded",
                        payload("ACCEPTED", "1", "JAVA").replace(USER, "nope"))))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
