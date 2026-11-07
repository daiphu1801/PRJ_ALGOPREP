package com.algoprep.problembank.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Responses of the learner-facing problem reads; field names are the wire names. */
public final class StudentProblemViews {
  private StudentProblemViews() {}

  public record PageMeta(int number, int size, long totalItems, int totalPages) {}

  public record Item(
      UUID id,
      String code,
      String title,
      List<String> topicNames,
      String levelCode,
      String levelDisplayName,
      BigDecimal acRate,
      boolean functionWrapperSupported,
      String solveState) {}

  /** Section 2. */
  public record ProblemList(List<Item> items, PageMeta page, boolean solveStateAvailable) {}

  /** Section 3: null when nothing matches. */
  public record RandomProblem(UUID problemId) {}

  public record SampleItem(
      UUID testcaseId,
      int order,
      String input,
      String inputDownloadUrl,
      String expectedOutput,
      String expectedOutputDownloadUrl) {}

  /** Section 8. */
  public record SampleList(List<String> parameterNames, List<SampleItem> items) {}
}
