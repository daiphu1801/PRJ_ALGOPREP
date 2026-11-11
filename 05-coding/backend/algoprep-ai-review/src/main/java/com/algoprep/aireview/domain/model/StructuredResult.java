package com.algoprep.aireview.domain.model;

/** Result of {@code completeStructured}; {@code usage} is null when the provider gave none. */
public record StructuredResult(String json, LlmUsage usage, String modelName) {}
