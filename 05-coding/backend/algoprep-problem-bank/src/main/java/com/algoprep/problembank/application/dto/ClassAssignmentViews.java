package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import java.util.List;
import java.util.UUID;

/** Responses of the instructor's class assignment screens; field names are the wire names. */
public final class ClassAssignmentViews {
  private ClassAssignmentViews() {}

  public record AssignedClass(UUID classId, String className) {}

  public record AssignedItem(
      UUID problemId,
      String title,
      List<String> topicNames,
      String levelCode,
      String levelDisplayName,
      List<AssignedClass> assignedClasses,
      Long submissionCount,
      Double acRatePercent,
      boolean withdrawn) {}

  /** Section 50. */
  public record AssignmentList(List<AssignedItem> items, PageMeta page) {}

  /** Section 51. */
  public record Summary(int assignedProblemCount, int classCount, Integer avgAcRatePercent) {}

  public record AssignableItem(
      UUID problemId,
      String code,
      String title,
      String levelCode,
      String levelDisplayName,
      List<String> topicNames) {}

  /** Section 52. */
  public record AssignableList(List<AssignableItem> items, PageMeta page) {}

  public record Failed(UUID id, String code, String message) {}

  /** Section 53. */
  public record AssignResult(
      List<UUID> succeededIds, List<UUID> skippedIds, List<Failed> failedItems) {}

  public record TopItem(UUID problemId, String title, long submissionCount) {}

  /** Section 55. */
  public record TopList(List<TopItem> items) {}
}
