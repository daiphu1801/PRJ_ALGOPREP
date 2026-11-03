package com.algoprep.problembank.domain.model;

/** Versioned persisted specification. */
public record StoredProblemSpec(ProblemSpecData data, int specVersion) {}
