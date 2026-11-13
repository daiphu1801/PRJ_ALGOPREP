package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.FeatureCode;
import com.algoprep.aireview.domain.model.ResponseGuards;
import java.util.UUID;

/** One feature of a publish request; a null field means "not sent" (DD api F5 section 17.2). */
public record PublishChange(
    FeatureCode feature,
    UUID expectedActivePromptId,
    String systemInstruction,
    String modelId,
    ResponseGuards guards) {

  @Override
  public String toString() {
    return "PublishChange[feature=" + feature + ", modelId=" + modelId + "]";
  }
}
