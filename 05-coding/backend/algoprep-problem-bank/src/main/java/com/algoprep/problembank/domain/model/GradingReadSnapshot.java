package com.algoprep.problembank.domain.model;

import java.util.List;

/** Spec, current version, approved metadata and its snapshot reference from one SELECT. */
public record GradingReadSnapshot(
    GradingSpecData data, List<TestcaseDescriptor> testcases, SnapshotMetadata snapshot) {
  public GradingReadSnapshot {
    testcases = List.copyOf(testcases);
  }
}
