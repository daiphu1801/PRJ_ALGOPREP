package com.algoprep.judge.application.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.judge.domain.model.WorkerControl;
import org.junit.jupiter.api.Test;

class ConfiguredWorkerScalingTest {
  @Test
  void customCapAndPollCountsSurviveTransactionalProposalCopies() {
    var scaling = new WorkerScaling(3, 1, 2);
    var busy = new WorkerControl(false, true, 5, 20);
    assertThat(scaling.poll(busy)).isEqualTo(2);
    scaling = scaling.copy();
    assertThat(scaling.poll(busy)).isEqualTo(3);
    for (int i = 0; i < 20; i++) {
      scaling = scaling.copy();
      assertThat(scaling.poll(busy)).isEqualTo(3);
    }
    var idle = new WorkerControl(false, true, 5, 0);
    assertThat(scaling.poll(idle)).isEqualTo(3);
    scaling = scaling.copy();
    assertThat(scaling.poll(idle)).isEqualTo(2);
    assertThat(scaling.poll(new WorkerControl(false, false, 5, 20))).isEqualTo(1);
  }

  @Test
  void invalidPollCountsFailBeforeStartingAConsumer() {
    assertThatThrownBy(() -> new WorkerScaling(8, 2, 0))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
