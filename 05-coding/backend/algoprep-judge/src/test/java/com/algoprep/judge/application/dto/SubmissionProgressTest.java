package com.algoprep.judge.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class SubmissionProgressTest {
  private static final UUID ID = UUID.randomUUID();
  private static final Instant NOW = Instant.parse("2026-11-03T05:00:00Z");
  private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();

  @Test
  void statusSequencesAndExactSchemaAreDeterministic() {
    var compiling = new SubmissionStatusProgress(ID, NOW, "COMPILING", null);
    var judging = new SubmissionStatusProgress(ID, NOW, "JUDGING", 12);
    assertThat(compiling.seq()).isEqualTo(1);
    assertThat(judging.seq()).isEqualTo(2);
    assertThat(compiling.isFinal()).isFalse();
    JsonNode body = json.valueToTree(judging);
    assertThat(body.propertyNames())
        .containsExactlyInAnyOrderElementsOf(
            Set.of("submissionId", "occurredAt", "type", "seq", "isFinal", "status", "totalCount"));
    assertThat(body.get("type").asString()).isEqualTo("STATUS_CHANGED");
    assertThat(body.get("occurredAt").asString()).isEqualTo(NOW.toString());
    assertThat(json.valueToTree(compiling).get("totalCount").isNull()).isTrue();
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"PENDING", "ACCEPTED", "compiling", ""})
  void rejectsUnsupportedStatus(String status) {
    assertThatThrownBy(() -> new SubmissionStatusProgress(ID, NOW, status, null))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsInvalidStatusCountsAndMissingIdentity() {
    assertThatThrownBy(() -> new SubmissionStatusProgress(ID, NOW, "JUDGING", null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SubmissionStatusProgress(ID, NOW, "JUDGING", 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SubmissionStatusProgress(ID, NOW, "COMPILING", 1))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new SubmissionStatusProgress(null, NOW, "COMPILING", null))
        .isInstanceOf(NullPointerException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"AC", "WA", "TLE", "MLE", "RE"})
  void testcaseSchemaCannotCarryHiddenContent(String verdict) {
    var event = new TestcaseProgress(ID, NOW, 3, verdict, 3, 2, 12);
    JsonNode body = json.valueToTree(event);
    assertThat(event.seq()).isEqualTo(5);
    assertThat(event.isFinal()).isFalse();
    assertThat(event.type()).isEqualTo("TESTCASE_FINISHED");
    assertThat(body.propertyNames())
        .containsExactlyInAnyOrder(
            "submissionId",
            "occurredAt",
            "type",
            "seq",
            "isFinal",
            "order",
            "verdict",
            "completedCount",
            "passedCount",
            "totalCount");
    assertThat(body.get("verdict").asString()).isEqualTo(verdict);
  }

  @Test
  void invalidTestcaseProgressIsRejected() {
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 0, "AC", 0, 0, 12))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 2, "AC", 1, 1, 12))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 1, "AC", 1, 2, 12))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 1, "AC", 1, -1, 12))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 1, "AC", 1, 1, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 1, "CE", 1, 1, 12))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 1, null, 1, 1, 12))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new TestcaseProgress(ID, NOW, 1, "AC", 1, 1, Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ACCEPTED",
        "WRONG_ANSWER",
        "TIME_LIMIT_EXCEEDED",
        "RUNTIME_ERROR",
        "COMPILE_ERROR",
        "SYSTEM_ERROR"
      })
  void finalSchemaNormalizesErrorScoresAndUsesKnownJudgingCount(String status) {
    var event = finished(status, 12);
    JsonNode body = json.valueToTree(event);
    assertThat(event.seq()).isEqualTo(15);
    assertThat(event.isFinal()).isTrue();
    assertThat(event.type()).isEqualTo("SUBMISSION_FINISHED");
    assertThat(body.propertyNames())
        .containsExactlyInAnyOrder(
            "submissionId",
            "occurredAt",
            "type",
            "seq",
            "isFinal",
            "status",
            "passedCount",
            "totalCount",
            "scoreRatio",
            "firstFailingOrder",
            "runtimeMs",
            "memoryKb",
            "finishedAt");
    if (status.equals("COMPILE_ERROR") || status.equals("SYSTEM_ERROR")) {
      assertThat(body.get("passedCount").isNull()).isTrue();
      assertThat(body.get("totalCount").isNull()).isTrue();
      assertThat(body.get("scoreRatio").isNull()).isTrue();
    } else {
      assertThat(body.get("scoreRatio").decimalValue()).isEqualByComparingTo("0.917");
    }
  }

  @Test
  void unknownCountFinishesAtThreeAndRejectsNonFinalStatusOrOverflow() {
    assertThat(finished("SYSTEM_ERROR", null).seq()).isEqualTo(3);
    assertThatThrownBy(() -> finished("JUDGING", 12)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> finished(null, 12)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> finished("SYSTEM_ERROR", 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> finished("SYSTEM_ERROR", Integer.MAX_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static SubmissionFinishedProgress finished(String status, Integer count) {
    return new SubmissionFinishedProgress(
        ID, NOW, status, 11, 12, new BigDecimal("0.917"), 3, 20L, 100L, NOW, count);
  }
}
