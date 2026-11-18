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
  public boolean equals(Object other) {
    return other instanceof GradingTestcase that
        && position == that.position
        && java.util.Objects.equals(testcaseId, that.testcaseId)
        && java.util.Objects.equals(visibility, that.visibility)
        && java.util.Arrays.equals(input, that.input)
        && java.util.Arrays.equals(expectedOutput, that.expectedOutput);
  }

  @Override
  public int hashCode() {
    int result = java.util.Objects.hash(testcaseId, visibility, position);
    result = 31 * result + java.util.Arrays.hashCode(input);
    return 31 * result + java.util.Arrays.hashCode(expectedOutput);
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
