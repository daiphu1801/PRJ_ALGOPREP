package com.algoprep.judge.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class GradingTestcaseTest {
  private static final UUID ID = UUID.randomUUID();

  private static GradingTestcase of(String input, String expected) {
    return new GradingTestcase(ID, "HIDDEN", 1, input.getBytes(), expected.getBytes());
  }

  @Test
  void equalityAndHashCodeFollowTheByteContent() {
    assertThat(of("1", "2")).isEqualTo(of("1", "2")).hasSameHashCodeAs(of("1", "2"));
    assertThat(of("1", "2")).isNotEqualTo(of("1", "3")).isNotEqualTo(of("9", "2"));
  }

  @Test
  void toStringNeverLeaksTheContent() {
    assertThat(of("secret-in", "secret-out").toString())
        .contains("HIDDEN")
        .doesNotContain("secret");
  }
}
