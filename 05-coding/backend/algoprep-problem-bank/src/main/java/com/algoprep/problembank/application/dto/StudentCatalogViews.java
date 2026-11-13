package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Responses of the learner's catalogue and bookmark reads; field names are the wire names. */
public final class StudentCatalogViews {
  private StudentCatalogViews() {}

  public record TopicProgressItem(
      UUID topicId, String topicName, int sortOrder, Integer solved, int total) {}

  /** Section 4. */
  public record TopicProgressList(List<TopicProgressItem> items, boolean solveStateAvailable) {}

  public record LevelProgressItem(
      String levelCode, String levelDisplayName, int sortOrder, int total, int solved) {}

  /** Section 14A. */
  public record MyLevelProgress(int totalPublished, int solved, List<LevelProgressItem> byLevel) {}

  public record BookmarkItem(
      UUID problemId,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      List<String> topicNames,
      String solveState,
      String note,
      Instant savedAt,
      boolean withdrawn) {}

  public record BookmarkMeta(int savedTotal, Integer solvedCount, int filteredTotal) {}

  /** Section 9. */
  public record BookmarkList(List<BookmarkItem> items, PageMeta page, BookmarkMeta meta) {}

  /** Sections 10: the saved bookmark. */
  public record BookmarkSavedView(UUID problemId, String note, Instant savedAt) {}

  /** Section 10 with the status code it needs: 201 when created, 200 when updated. */
  public record BookmarkSaveResult(BookmarkSavedView bookmark, boolean created) {}

  /** Section 12. */
  public record BookmarkState(boolean bookmarked) {}
}
