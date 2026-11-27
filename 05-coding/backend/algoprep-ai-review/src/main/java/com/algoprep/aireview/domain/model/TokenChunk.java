package com.algoprep.aireview.domain.model;

/** One streamed piece; only the last chunk carries {@code usage}. */
public record TokenChunk(String text, LlmUsage usage) {}
