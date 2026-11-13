package com.algoprep.aireview.domain.model;

import java.time.Duration;

/** Per-call limits (DD F5 section 3.1, Bang 4). */
public record CallOptions(
    String modelId, int maxTokens, Duration firstTokenTimeout, Duration totalTimeout) {
  /** Output cap shown read-only on the prompt admin screen (DD api F5 section 14.3). */
  public static final int DEFAULT_MAX_TOKENS = 4096;
}
