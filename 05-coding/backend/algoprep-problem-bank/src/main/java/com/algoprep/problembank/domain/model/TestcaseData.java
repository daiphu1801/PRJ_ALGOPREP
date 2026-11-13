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
