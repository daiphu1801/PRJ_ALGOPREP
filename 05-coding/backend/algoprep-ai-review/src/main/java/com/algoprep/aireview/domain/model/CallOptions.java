package com.algoprep.aireview.domain.model;

import java.time.Duration;

/** Per-call limits (DD F5 section 3.1, Bang 4). */
public record CallOptions(
    String modelId, int maxTokens, Duration firstTokenTimeout, Duration totalTimeout) {
  /** Output cap shown read-only on the prompt admin screen (DD api F5 section 14.3). */
  public static final int DEFAULT_MAX_TOKENS = 4096;

  /**
   * Output cap for testcase-script generation. Reasoning models spend part of the cap on hidden
   * reasoning tokens; at 4096 a live probe returned an empty body ({@code finish_reason=length}),
   * at 8192 a valid script (finding F-01, 06-plan/reports/261114-1150-ai-nghiem-thu-t03-t04).
   */
  public static final int GENERATE_TESTCASE_MAX_TOKENS = 8192;

  /** The output cap a feature really sends; also what the admin prompt screen shows read-only. */
  public static int maxTokensFor(FeatureCode feature) {
    return feature == FeatureCode.GENERATE_TESTCASE
        ? GENERATE_TESTCASE_MAX_TOKENS
        : DEFAULT_MAX_TOKENS;
  }
}
