package com.algoprep.harness.domain.model;

/** A sanitized location in the learner's source; never includes compiler context lines. */
public record CompileDiagnostic(int line, Integer column, String message) {}
