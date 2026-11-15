package com.algoprep.judge.application.dto.execution;

import com.algoprep.judge.application.dto.GradingTestcase;
import java.util.List;
import java.util.UUID;

public record GradingTestcaseSnapshot(
    UUID problemId, int version, List<GradingTestcase> testcases) {
  public GradingTestcaseSnapshot {
    testcases = List.copyOf(testcases);
  }
}
