package com.algoprep.problembank.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class TestcaseDataTest {
  private static final UUID ID = UUID.randomUUID();

  private static TestcaseData of(String input, String expected) {
    return new TestcaseData(ID, "HIDDEN", 1, input.getBytes(), expected.getBytes());
  }

  @Test
  void equalityAndHashCodeFollowTheByteContent() {
    assertThat(of("1", "2")).isEqualTo(of("1", "2")).hasSameHashCodeAs(of("1", "2"));
    assertThat(of("1", "2")).isNotEqualTo(of("1", "3")).isNotEqualTo(of("9", "2"));
  }

  @Test
  void toStringNeverLeaksTheContent() {
    assertThat(of("secret-in", "secret-out").toString()).doesNotContain("secret");
  }
}
