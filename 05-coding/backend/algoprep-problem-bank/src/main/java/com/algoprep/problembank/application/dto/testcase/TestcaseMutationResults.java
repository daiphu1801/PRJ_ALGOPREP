package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Responses of the testcase authoring use case; field names are the wire names. */
public final class TestcaseMutationResults {
  private TestcaseMutationResults() {}

  /** Sections 42 and 43. */
  public record Saved(
      TestcaseView testcase, int currentTestcaseSetVersion, Instant problemUpdatedAt) {}

  /** Section 44. */
  public record Removed(int currentTestcaseSetVersion, Instant problemUpdatedAt) {}

  /** Section 45. */
  public record Ordered(
      List<Position> items, Instant problemUpdatedAt, int currentTestcaseSetVersion) {}

  public record Position(UUID id, int position) {}
}
