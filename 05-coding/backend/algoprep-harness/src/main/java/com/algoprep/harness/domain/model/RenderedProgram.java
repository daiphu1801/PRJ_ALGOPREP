package com.algoprep.harness.domain.model;

/** Source and its regions only; not a prepared judge payload or a versioned problem snapshot. */
public record RenderedProgram(String sourceFileName, String text, LineMap lineMap) {}
