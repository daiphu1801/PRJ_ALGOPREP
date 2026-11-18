package com.algoprep.common.text;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EdgeTrimTest {

  @Test
  void edgesStripsBothEndsAndKeepsTheMiddle() {
    assertThat(EdgeTrim.edges("__a_b__", "_")).isEqualTo("a_b");
    assertThat(EdgeTrim.edges(" \t a b\t ", " \t")).isEqualTo("a b");
  }

  @Test
  void edgesOfAllStrippedOrEmptyInputIsEmpty() {
    assertThat(EdgeTrim.edges("____", "_")).isEmpty();
    assertThat(EdgeTrim.edges("", "_")).isEmpty();
  }

  @Test
  void trailingLeavesTheStartUntouched() {
    assertThat(EdgeTrim.trailing("//a//", "/")).isEqualTo("//a");
    assertThat(EdgeTrim.trailing("abc", "/")).isEqualTo("abc");
    assertThat(EdgeTrim.trailing("\n\n", "\n")).isEmpty();
  }

  @Test
  void aLongRunIsHandledInLinearTime() {
    String run = "_".repeat(200_000) + "x";
    assertThat(EdgeTrim.edges(run, "_")).isEqualTo("x");
  }
}
