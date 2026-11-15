package com.algoprep.problembank.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Content owned by F2; explicit resource limits are resolved by the caller before draft creation.
 */
public record ProblemContentData(
    String title,
    String statementMd,
    String constraintsMd,
    UUID levelId,
    Limits limits,
    List<UUID> topicIds,
    List<String> tags,
    List<Example> workedExamples,
    Solution referenceSolution) {
  public ProblemContentData {
    topicIds = copy(topicIds);
    tags = copy(tags);
    workedExamples = copy(workedExamples);
  }

  private static <T> List<T> copy(List<T> values) {
    return values == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(values));
  }

  public record Limits(
      int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, Integer maxSubmissionsPerHour) {}

  public record Example(UUID id, int position, String input, String output, String explanationMd) {}

  public record Solution(String language, String sourceCode) {}
}
