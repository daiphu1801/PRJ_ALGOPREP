package com.algoprep.judge.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Raw F2 values, grouped internally without exposing any testcase bytes or object references. */
public record GradingSpecification(
    Problem problem,
    Limits limits,
    Versions versions,
    Matching matching,
    List<Testcase> testcases) {
  public GradingSpecification {
    testcases = List.copyOf(testcases);
  }

  public record Problem(
      UUID problemId, boolean published, boolean functionWrapperSupported, boolean snapshotReady) {}

  public record Limits(
      int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, Integer maxSubmissionsPerHour) {}

  public record Versions(int specVersion, int currentTestcaseSetVersion) {}

  public record Matching(String matchingStrategy, BigDecimal epsilonValue) {}

  public record Testcase(UUID testcaseId, String visibility, int position) {}
}
