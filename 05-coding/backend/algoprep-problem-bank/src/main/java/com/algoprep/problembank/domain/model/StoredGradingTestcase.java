package com.algoprep.problembank.domain.model;

/** References for an approved Sample or Hidden testcase, read under the parent publication lock. */
public record StoredGradingTestcase(
    TestcaseDescriptor descriptor, Content input, Content expectedOutput) {
  public record Content(String storage, String inline, String objectKey) {}
}
