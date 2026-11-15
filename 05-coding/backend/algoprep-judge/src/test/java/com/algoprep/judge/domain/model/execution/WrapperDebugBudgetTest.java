package com.algoprep.judge.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class WrapperDebugBudgetTest {
  private static final String SENTINEL = "@@APH-RESULT@@";
  private static final String FRAME = "\n" + SENTINEL + "\n";

  static Stream<Arguments> rawPrefixes() {
    return Stream.of(
            "",
            "x".repeat(4095),
            "x".repeat(4096),
            "x".repeat(4097),
            "é".repeat(2048),
            "é".repeat(2048) + "x",
            "€".repeat(1365) + "x",
            "€".repeat(1365) + "xx",
            "😀".repeat(1024),
            "😀".repeat(1024) + "x",
            "\r\n".repeat(2048),
            "\r\n".repeat(2048) + "x",
            "x".repeat(4095) + "\r",
            "x".repeat(4096) + "\r",
            "x".repeat(4095) + "\uD800",
            "x".repeat(4096) + "\uDC00")
        .map(prefix -> Arguments.of(prefix, prefix.getBytes(StandardCharsets.UTF_8).length > 4096));
  }

  @ParameterizedTest
  @MethodSource("rawPrefixes")
  void countsOriginalUtf8BytesAndExcludesExactlyOneTrustedLf(String prefix, boolean exceeded) {
    assertThat(WrapperDebugBudget.exceeded(prefix + FRAME + "7\n", SENTINEL)).isEqualTo(exceeded);
  }

  @Test
  void lastMarkerAndRawLineBoundariesAgreeWithP4Selection() {
    String earlier = "\n" + SENTINEL + "\n";
    assertThat(WrapperDebugBudget.exceeded(earlier + "x".repeat(4096) + FRAME + "7", SENTINEL))
        .isTrue();
    assertThat(WrapperDebugBudget.exceeded("x".repeat(4096) + "\r" + SENTINEL + "\r7", SENTINEL))
        .isTrue();
    assertThat(WrapperDebugBudget.exceeded("x".repeat(4095) + "\r" + SENTINEL + "\r7", SENTINEL))
        .isFalse();
    assertThat(WrapperDebugBudget.exceeded("x".repeat(4096) + "\n" + SENTINEL, SENTINEL)).isFalse();
    assertThat(WrapperDebugBudget.exceeded(SENTINEL + "\n7", SENTINEL)).isFalse();
  }

  @Test
  void missingOrNonLineMarkerRetainsNormalP4Semantics() {
    assertThat(WrapperDebugBudget.exceeded("x".repeat(5000), SENTINEL)).isFalse();
    assertThat(WrapperDebugBudget.exceeded("x".repeat(5000) + SENTINEL + "\n7", SENTINEL))
        .isFalse();
    assertThat(WrapperDebugBudget.exceeded("x".repeat(5000) + FRAME, null)).isFalse();
    assertThat(WrapperDebugBudget.exceeded("x".repeat(5000) + FRAME, "")).isFalse();
  }
}
