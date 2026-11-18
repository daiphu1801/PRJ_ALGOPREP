package com.algoprep.aireview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CallOptionsTest {

  @Test
  void testcaseGenerationGetsTheLargerCapAndEveryOtherFeatureTheDefault() {
    assertThat(CallOptions.maxTokensFor(FeatureCode.GENERATE_TESTCASE))
        .isEqualTo(CallOptions.GENERATE_TESTCASE_MAX_TOKENS)
        .isGreaterThan(CallOptions.DEFAULT_MAX_TOKENS);
    for (FeatureCode feature : FeatureCode.values()) {
      if (feature != FeatureCode.GENERATE_TESTCASE) {
        assertThat(CallOptions.maxTokensFor(feature)).isEqualTo(CallOptions.DEFAULT_MAX_TOKENS);
      }
    }
  }
}
