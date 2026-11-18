package com.algoprep.bootstrap.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

/** The deadline wrapper of the identity bridges: a slow or failing read is one plain error. */
class BridgeDeadlineTest {

  @Test
  void anAnswerInTimeIsReturned() {
    assertThat(BridgeDeadline.call("t", Duration.ofSeconds(2), () -> 42)).isEqualTo(42);
  }

  @Test
  void aFailureBecomesOneErrorWithoutTheCause() {
    assertThatThrownBy(
            () ->
                BridgeDeadline.call(
                    "t",
                    Duration.ofSeconds(2),
                    () -> {
                      throw new IllegalArgumentException("secret detail");
                    }))
        .isInstanceOf(IllegalStateException.class)
        .hasNoCause()
        .hasMessageNotContaining("secret");
  }

  @Test
  void aMissedDeadlineBecomesOneErrorAndStopsTheWorker() {
    assertThatThrownBy(
            () ->
                BridgeDeadline.call(
                    "t",
                    Duration.ofMillis(50),
                    () -> {
                      Thread.sleep(5_000);
                      return 1;
                    }))
        .isInstanceOf(IllegalStateException.class);
  }
}
