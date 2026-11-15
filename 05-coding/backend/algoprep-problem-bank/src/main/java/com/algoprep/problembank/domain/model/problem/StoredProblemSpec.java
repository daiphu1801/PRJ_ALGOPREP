package com.algoprep.problembank.domain.model.problem;

/** Versioned persisted specification. */
public record StoredProblemSpec(ProblemSpecData data, int specVersion) {}
