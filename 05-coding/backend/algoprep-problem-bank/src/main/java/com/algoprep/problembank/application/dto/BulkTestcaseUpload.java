package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.ProblemAuthoringView.TestcaseView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Inputs and outputs of the bulk testcase upload (DD F2 sections 46 and 47); wire names. */
public final class BulkTestcaseUpload {
  private BulkTestcaseUpload() {}

  /** One pair the browser matched; {@code visibility} null means HIDDEN. */
  public record Item(
      String clientKey,
      long inputSizeBytes,
      long expectedOutputSizeBytes,
      String visibility,
      String category) {}

  public record RequestUrls(AuthoringScope scope, UUID problemId, List<Item> items) {}

  public record Target(String putUrl) {}

  public record UrlItem(String clientKey, UUID testcaseId, Target input, Target expectedOutput) {}

  /** Section 46 response. */
  public record UrlPlan(UUID uploadId, Instant expiresAt, List<UrlItem> items) {}

  public record Confirm(AuthoringScope scope, UUID problemId, UUID uploadId) {}

  /** Section 47 response. */
  public record Added(
      List<TestcaseView> added, int currentTestcaseSetVersion, Instant problemUpdatedAt) {}
}
