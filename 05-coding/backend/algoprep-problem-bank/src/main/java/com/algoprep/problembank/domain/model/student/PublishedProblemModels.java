package com.algoprep.problembank.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Read models of the learner-facing problem reads (DD F2 sections 2, 3, 6, 7, 8). */
public final class PublishedProblemModels {
  private PublishedProblemModels() {}

  /** Problems the caller solved or tried, as the identity module reports them. */
  public record SolveStates(Set<UUID> solved, Set<UUID> attempted) {}

  /** Already normalised list filter; a null field means "no condition". */
  public record Filter(String q, String status, String difficulty, UUID topic, String tag) {}

  /** Normalised paging and order: {@code sort} is code, level or acRate. */
  public record Paging(String sort, boolean ascending, int page, int pageSize) {}

  public record Row(
      UUID id,
      String code,
      String title,
      List<String> topicNames,
      String levelCode,
      String levelDisplayName,
      BigDecimal acRate,
      boolean functionWrapperSupported) {}

  public record Page(List<Row> items, long totalItems, int page) {}

  public record Example(int order, String input, String output, String explanationMd) {}

  public record Workspace(
      UUID id,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      List<String> topicNames,
      String statementMd,
      String constraintsMd,
      boolean functionWrapperSupported,
      String stdinFormatMd,
      String stdoutFormatMd,
      BigDecimal acRate,
      boolean bookmarked,
      List<Example> workedExamples) {}

  public record Overview(
      UUID id,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      String statementMd,
      String constraintsMd,
      List<String> topicNames,
      BigDecimal acRate,
      int hiddenTestcaseCount,
      boolean bookmarked,
      List<Example> examples) {}

  public record Sample(
      UUID testcaseId,
      int position,
      StoredGradingTestcase.Content input,
      StoredGradingTestcase.Content expectedOutput) {}

  public record Samples(List<String> parameterNames, List<Sample> items) {}
}
