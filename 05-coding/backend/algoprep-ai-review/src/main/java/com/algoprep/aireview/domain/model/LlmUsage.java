package com.algoprep.aireview.domain.model;

/** Token counts reported by the provider. When the provider reports none, there is no LlmUsage. */
public record LlmUsage(int promptTokens, int completionTokens) {
  public LlmUsage {
    if (promptTokens < 0 || completionTokens < 0) {
      throw new IllegalArgumentException("token counts must not be negative");
    }
  }
}
