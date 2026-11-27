package com.algoprep.problembank.domain.model.grading;

import com.algoprep.problembank.domain.model.snapshot.SnapshotMetadata;
import com.algoprep.problembank.domain.model.testcase.TestcaseDescriptor;
import java.util.List;

/** Spec, current version, approved metadata and its snapshot reference from one SELECT. */
public record GradingReadSnapshot(
    GradingSpecData data, List<TestcaseDescriptor> testcases, SnapshotMetadata snapshot) {
  public GradingReadSnapshot {
    testcases = List.copyOf(testcases);
  }
}
