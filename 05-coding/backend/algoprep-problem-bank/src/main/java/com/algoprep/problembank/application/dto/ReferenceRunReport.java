package com.algoprep.problembank.application.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Input and output of "run the sample solution" (DD F2 section 48); wire names. The report holds
 * verdicts and numbers only: no actual output, no diff, not even for a HIDDEN testcase (rule 1.4).
 */
public final class ReferenceRunReport {
  private ReferenceRunReport() {}

  /** {@code testcaseId}, {@code language} and {@code sourceCode} are each optional. */
  public record Command(
      AuthoringScope scope, UUID problemId, UUID testcaseId, String language, String sourceCode) {
    @Override
    public String toString() {
      return "ReferenceRunReport.Command[" + problemId + "]";
    }
  }

  public record Summary(int passed, int total, boolean allPass) {}

  /** {@code failure} is null for a PASS. */
  public record Row(
      UUID testcaseId,
      int position,
      String visibility,
      String verdict,
      String failure,
      Integer runtimeMs,
      Integer memoryKb) {}

  public record Report(String compileError, Summary summary, List<Row> results, Instant ranAt) {}
}
