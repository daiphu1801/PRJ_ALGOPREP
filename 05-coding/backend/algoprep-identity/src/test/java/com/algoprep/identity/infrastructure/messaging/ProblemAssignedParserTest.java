package com.algoprep.identity.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.domain.model.ClassProblemAssignment;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The Outbox envelope of DD api F2 section 53, as the problem-bank relay publishes it. */
class ProblemAssignedParserTest {
  private static final String EVENT = "11111111-1111-1111-1111-111111111111";
  private static final String CLASS_ID = "22222222-2222-2222-2222-222222222222";
  private static final String PROBLEM = "33333333-3333-3333-3333-333333333333";
  private static final String ACTOR = "44444444-4444-4444-4444-444444444444";

  private static byte[] body(String type, String occurredAt, String classId) {
    return ("{\"eventId\":\""
            + EVENT
            + "\",\"eventType\":\""
            + type
            + "\",\"occurredAt\":\""
            + occurredAt
            + "\",\"schemaVersion\":1,\"payload\":{\"problemId\":\""
            + PROBLEM
            + "\",\"classId\":\""
            + classId
            + "\",\"actorUserId\":\""
            + ACTOR
            + "\",\"problemTitle\":\"Two Sum\",\"assignedAt\":\"2026-11-07T08:00:00Z\"}}")
        .getBytes(StandardCharsets.UTF_8);
  }

  @Test
  void readsTheFieldsTheFeedNeedsAndTakesTheTimeFromTheEnvelope() {
    ClassProblemAssignment a =
        ProblemAssignedParser.parse(
            body("ProblemAssignedToClass", "2026-11-07T08:00:00Z", CLASS_ID));

    assertThat(a.eventId()).isEqualTo(UUID.fromString(EVENT));
    assertThat(a.classId()).isEqualTo(UUID.fromString(CLASS_ID));
    assertThat(a.problemId()).isEqualTo(UUID.fromString(PROBLEM));
    assertThat(a.actorUserId()).isEqualTo(UUID.fromString(ACTOR));
    assertThat(a.occurredAt()).isEqualTo(Instant.parse("2026-11-07T08:00:00Z"));
  }

  @Test
  void rejectsWhatCanNeverSucceedOnARetry() {
    assertThatThrownBy(
            () ->
                ProblemAssignedParser.parse(
                    body("ProblemUnassignedFromClass", "2026-11-07T08:00:00Z", CLASS_ID)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                ProblemAssignedParser.parse(body("ProblemAssignedToClass", "yesterday", CLASS_ID)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                ProblemAssignedParser.parse(
                    body("ProblemAssignedToClass", "2026-11-07T08:00:00Z", "not-a-uuid")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> ProblemAssignedParser.parse("[]".getBytes(StandardCharsets.UTF_8)))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
