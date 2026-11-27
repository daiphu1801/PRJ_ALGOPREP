package com.algoprep.judge.application.dto.read;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Wire shape of the F4-14 queue object on GET /submissions/{id}/progress (AC-78, AC-80). */
class ProgressQueueJsonTest {
  private final JsonMapper mapper = JsonMapper.builder().build();

  private SubmissionReads.Progress progress(String status, SubmissionReads.QueueInfo queue) {
    return new SubmissionReads.Progress(
        UUID.randomUUID(), status, 0, false, null, 0, 0, List.of(), null, null, queue);
  }

  @Test
  void queueSerialisesAsAheadAndEstimatedWaitSeconds() {
    var json =
        mapper.readTree(
            mapper.writeValueAsString(progress("PENDING", new SubmissionReads.QueueInfo(18, 9))));
    assertThat(json.get("queue").get("ahead").asInt()).isEqualTo(18);
    assertThat(json.get("queue").get("estimatedWaitSeconds").asInt()).isEqualTo(9);
    assertThat(json.get("queue").size()).isEqualTo(2);
  }

  @Test
  void unknownEstimateIsExplicitNullAndNonPendingQueueIsNull() {
    var pending =
        mapper.readTree(
            mapper.writeValueAsString(
                progress("PENDING", new SubmissionReads.QueueInfo(18, null))));
    assertThat(pending.get("queue").get("estimatedWaitSeconds").isNull()).isTrue();
    var judging = mapper.readTree(mapper.writeValueAsString(progress("JUDGING", null)));
    assertThat(judging.has("queue")).isTrue();
    assertThat(judging.get("queue").isNull()).isTrue();
  }
}
