package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** One generation run kept as provenance (BR-16 step 9); script text never reaches toString. */
public record TestcaseGenerationRun(
    UUID id,
    UUID problemId,
    String generatorScript,
    String validatorScript,
    long seed,
    String modelId,
    String promptVersion,
    UUID requestedBy,
    int addedCount,
    String droppedJson) {
  @Override
  public String toString() {
    return "TestcaseGenerationRun[" + id + "]";
  }
}
