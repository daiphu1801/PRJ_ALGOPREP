package com.algoprep.aireview.domain.model;

import java.util.Objects;

/**
 * The admin-editable part of a template. The instruction is configuration data and is never logged;
 * {@code guards} is null only for GENERATE_TESTCASE (DB5:25).
 */
public record PromptContent(String systemInstruction, String modelId, ResponseGuards guards) {
  public PromptContent {
    Objects.requireNonNull(systemInstruction, "systemInstruction");
    Objects.requireNonNull(modelId, "modelId");
  }

  @Override
  public String toString() {
    return "PromptContent[modelId=" + modelId + ", guards=" + guards + "]";
  }
}
