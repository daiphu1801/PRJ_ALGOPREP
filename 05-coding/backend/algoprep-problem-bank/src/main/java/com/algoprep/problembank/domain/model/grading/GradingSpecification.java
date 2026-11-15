package com.algoprep.problembank.domain.model;

import java.util.List;

public record GradingSpecification(
    GradingSpecData data, List<TestcaseDescriptor> testcases, boolean snapshotReady) {
  public GradingSpecification {
    testcases = List.copyOf(testcases);
  }
}
