package com.algoprep.aireview.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable copy of the ACTIVE prompt template, read exactly once at the start of an LLM call and
 * used for cache lookup, composing, model choice, schema validation and persistence (BR-AI-01 step
 * 1). Every field is immutable, so a publish that lands mid-call cannot change this call.
 */
public record PromptSnapshot(
    UUID id,
    FeatureCode featureCode,
    String systemInstruction,
    ResponseGuards guards,
    String modelId,
    String responseSchemaVersion) {
  public PromptSnapshot {
    Objects.requireNonNull(id, "id");
    Objects.requireNonNull(featureCode, "featureCode");
    Objects.requireNonNull(systemInstruction, "systemInstruction");
    Objects.requireNonNull(modelId, "modelId");
    Objects.requireNonNull(responseSchemaVersion, "responseSchemaVersion");
    if (systemInstruction.isBlank()) {
      throw new IllegalArgumentException("systemInstruction must not be blank");
    }
    guards = guards == null ? ResponseGuards.NONE : guards;
  }
}
