package com.algoprep.problembank.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProblemCodeTest {
  @Test
  void slugsRemoveAccentsBoundSuffixAndHaveProblemFallback() {
    assertThat(ProblemCode.from("Đếm số phần tử!", 1)).isEqualTo("DEM-SO-PHAN-TU");
    assertThat(ProblemCode.from("! !", 1)).isEqualTo("PROBLEM");
    assertThat(ProblemCode.from("! !", 2)).isEqualTo("PROBLEM-2");
    assertThat(ProblemCode.from("a".repeat(63) + " b", 20)).hasSize(64).endsWith("-20");
  }
}
