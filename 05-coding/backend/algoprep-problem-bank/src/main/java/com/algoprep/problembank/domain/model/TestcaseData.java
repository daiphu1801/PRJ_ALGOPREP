package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** Trusted backend content. Arrays are copied and the string representation excludes content. */
public record TestcaseData(
    UUID testcaseId, String visibility, int position, byte[] input, byte[] expectedOutput) {
  public TestcaseData {
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
    return other instanceof TestcaseData that
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
    return "TestcaseData[testcaseId="
        + testcaseId
        + ", visibility="
        + visibility
        + ", position="
        + position
        + "]";
  }
}
