package com.algoprep.problembank.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** One coherent database view. Limits retain database units; null quota is not defaulted here. */
public record GradingSpecData(
    Problem problem, Limits limits, Versions versions, Matching matching) {
  public record Problem(UUID problemId, boolean published, boolean functionWrapperSupported) {}

  public record Limits(
      int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, Integer maxSubmissionsPerHour) {}

  public record Versions(int specVersion, int currentTestcaseSetVersion) {}

  public record Matching(String matchingStrategy, BigDecimal epsilonValue) {}
}
