package com.algoprep.bootstrap.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.aireview.application.dto.InterviewTranscript;
import com.algoprep.aireview.application.ports.in.InterviewTranscriptReadUseCase;
import com.algoprep.aireview.domain.model.InterviewEntryType;
import com.algoprep.aireview.domain.model.InterviewSessionStage;
import com.algoprep.aireview.domain.model.InterviewStage;
import com.algoprep.aireview.domain.model.InterviewTurn;
import com.algoprep.aireview.domain.model.InterviewerLevel;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Field mapping and failure shape of the transcript export bridge, without a database. */
class InterviewTranscriptExportBridgeTest {
  private static final Instant AT = Instant.parse("2026-01-01T00:00:00Z");

  private static InterviewTranscript transcript(InterviewerLevel level) {
    return new InterviewTranscript(
        UUID.randomUUID(),
        InterviewEntryType.TOPIC_SELECTION,
        InterviewSessionStage.COMPLETED,
        AT,
        AT,
        List.of("graphs"),
        null,
        null,
        level,
        List.of(
            new InterviewTranscript.Turn(
                1, InterviewTurn.Role.AI, InterviewStage.EXPLAIN, "q", AT)));
  }

  @Test
  void everyFieldIsMappedAndANullLevelStaysNull() {
    var first = transcript(InterviewerLevel.JUNIOR);
    var second = transcript(null);
    var bridge =
        new InterviewTranscriptExportBridge(user -> List.of(first, second), Duration.ofSeconds(2));

    var export = bridge.readAll(UUID.randomUUID());

    assertThat(export.sessions()).hasSize(2);
    var mapped = export.sessions().get(0);
    assertThat(mapped.sessionId()).isEqualTo(first.sessionId());
    assertThat(mapped.entryType()).isEqualTo("TOPIC_SELECTION");
    assertThat(mapped.stage()).isEqualTo("COMPLETED");
    assertThat(mapped.interviewerLevel()).isEqualTo("JUNIOR");
    assertThat(mapped.topics()).containsExactly("graphs");
    assertThat(mapped.turns().get(0).role()).isEqualTo("AI");
    assertThat(mapped.turns().get(0).stage()).isEqualTo("EXPLAIN");
    assertThat(mapped.turns().get(0).content()).isEqualTo("q");
    assertThat(export.sessions().get(1).interviewerLevel()).isNull();
  }

  @Test
  void aFailureIsOneIllegalStateExceptionWithoutCause() {
    InterviewTranscriptReadUseCase broken =
        user -> {
          throw new IllegalArgumentException("ai secret detail");
        };
    var bridge = new InterviewTranscriptExportBridge(broken, Duration.ofSeconds(2));

    assertThatThrownBy(() -> bridge.readAll(UUID.randomUUID()))
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessageNotContaining("secret");
  }
}
