package com.algoprep.problembank.domain.model;

import java.util.List;
import java.util.UUID;

public record TestcaseSnapshot(UUID problemId, int version, List<TestcaseData> testcases) {
  public TestcaseSnapshot {
    testcases = List.copyOf(testcases);
  }
}
