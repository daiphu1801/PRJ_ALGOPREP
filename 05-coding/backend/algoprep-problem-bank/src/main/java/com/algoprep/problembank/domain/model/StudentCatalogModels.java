package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read models of the learner's progress, catalogue summary and bookmarks (DD F2 sections 4, 9 to
 * 14A).
 */
public final class StudentCatalogModels {
  private StudentCatalogModels() {}

  /** {@code solved} is null when the solve state could not be read. */
  public record TopicProgress(
      UUID topicId, String name, int sortOrder, int total, Integer solved) {}

  public record LevelProgress(
      String code, String displayName, int sortOrder, int total, int solved) {}

  public record TopicCount(UUID topicId, String name, int count) {}

  public record LevelCount(String code, String displayName, int sortOrder, int count) {}

  public record CatalogSummary(
      int totalPublished, List<TopicCount> byTopic, List<LevelCount> byLevel) {}

  /** One saved problem; {@code withdrawn} is true for an unpublished or deleted problem. */
  public record Bookmark(
      UUID problemId,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      List<String> topicNames,
      String note,
      Instant savedAt,
      boolean withdrawn) {}

  public record BookmarkSaved(String note, Instant savedAt, boolean created) {}
}
