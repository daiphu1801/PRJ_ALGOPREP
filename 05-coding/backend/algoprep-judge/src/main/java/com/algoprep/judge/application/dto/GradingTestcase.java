package com.algoprep.judge.application.dto;

import java.util.UUID;

/** Trusted internal bytes only; neither HTTP responses nor queue messages use this DTO. */
public record GradingTestcase(
    UUID testcaseId, String visibility, int position, byte[] input, byte[] expectedOutput) {
  public GradingTestcase {
    input = input.clone();
    expectedOutput = expectedOutput.clone();
  }

  @Override
  public byte[] input() {
    return input.clone();
  }

  @Override
  public byte[] expectedOutput() {
    return expectedOutput.clone();
  }

  @Override
  public String toString() {
    return "GradingTestcase[testcaseId="
        + testcaseId
        + ", visibility="
        + visibility
        + ", position="
        + position
        + "]";
  }
}
