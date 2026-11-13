package com.algoprep.problembank.application.dto;

import java.util.List;
import java.util.UUID;

/** Inputs of the testcase authoring use case (DD F2 sections 42 to 45). */
public final class TestcaseCommands {
  private TestcaseCommands() {}

  /** Section 42; {@code visibility} null means HIDDEN. */
  public record Create(
      AuthoringScope scope,
      UUID problemId,
      String visibility,
      String input,
      String expectedOutput,
      String category) {}

  /**
   * Section 43: only what the client sent is changed. {@code categoryPresent} tells an explicit
   * {@code null} (clear it) from a missing key (leave it).
   */
  public record Patch(
      String visibility,
      String input,
      String expectedOutput,
      boolean categoryPresent,
      String category,
      Boolean approved) {}

  public record Update(AuthoringScope scope, UUID problemId, UUID testcaseId, Patch patch) {}

  public record Delete(AuthoringScope scope, UUID problemId, UUID testcaseId) {}

  public record Reorder(AuthoringScope scope, UUID problemId, List<UUID> orderedIds) {}
}
