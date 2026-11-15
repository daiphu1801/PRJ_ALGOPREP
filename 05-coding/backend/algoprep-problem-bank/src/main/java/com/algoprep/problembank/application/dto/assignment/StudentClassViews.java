package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.application.dto.StudentProblemViews.PageMeta;
import com.algoprep.problembank.domain.model.StudentClassModels.ClassSolveState;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Responses of the learner's class assignment reads; field names are the wire names. */
public final class StudentClassViews {
  private StudentClassViews() {}

  /** {@code solveState} is null when the judge module could not answer. */
  public record MyAssignmentItem(
      UUID problemId, String title, ClassSolveState solveState, Instant assignedAt) {}

  public record MyAssignmentGroup(UUID classId, String className, List<MyAssignmentItem> items) {}

  /** Section 5. */
  public record MyAssignmentGroups(List<MyAssignmentGroup> groups) {}

  public record MyClassProblem(
      UUID classId,
      UUID problemId,
      String code,
      String title,
      String levelDisplayName,
      ClassSolveState solveState,
      Instant assignedAt,
      boolean withdrawn) {}

  /**
   * Class-wide totals of section 5A over every row the list would show, not just the page. {@code
   * solvedCount} is null when the judge module could not answer for the whole set.
   */
  public record ClassProblemsMeta(int totalAssigned, Integer solvedCount) {}

  /** Section 5A, one page of the caller's rows plus the totals over all pages. */
  public record MyClassProblems(
      List<MyClassProblem> items, PageMeta page, ClassProblemsMeta meta) {}
}
