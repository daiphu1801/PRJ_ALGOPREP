package com.algoprep.aireview.domain.model.prompt;

import com.algoprep.aireview.domain.exception.AiInvalidRequestException;
import com.algoprep.aireview.domain.model.CallOptions;
import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ResponseGuards;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * The admin-editable part of a template. The instruction is configuration data and is never logged;
 * {@code guards} is null only for GENERATE_TESTCASE (DB5:25).
 */
public record PromptContent(
    String systemInstruction, String modelId, ResponseGuards guards, Integer maxTokens) {
  public static final int MAX_OUTPUT_TOKENS = 32_768;

  public PromptContent(String systemInstruction, String modelId, ResponseGuards guards) {
    this(systemInstruction, modelId, guards, null);
  }

  public PromptContent {
    Objects.requireNonNull(systemInstruction, "systemInstruction");
    Objects.requireNonNull(modelId, "modelId");
  }

  public int effectiveMaxTokens(FeatureCode feature) {
    return maxTokens == null ? CallOptions.maxTokensFor(feature) : maxTokens;
  }

  public void validateMaxTokens(FeatureCode feature, OptionalInt modelLimit) {
    int limit = Math.min(MAX_OUTPUT_TOKENS, modelLimit.orElse(MAX_OUTPUT_TOKENS));
    int value = effectiveMaxTokens(feature);
    if (value < 1 || value > limit) {
      throw new AiInvalidRequestException("AIR-V40", "maxTokens must be between 1 and " + limit);
    }
  }

  @Override
  public String toString() {
    return "PromptContent[modelId=" + modelId + ", guards=" + guards + "]";
  }
}
