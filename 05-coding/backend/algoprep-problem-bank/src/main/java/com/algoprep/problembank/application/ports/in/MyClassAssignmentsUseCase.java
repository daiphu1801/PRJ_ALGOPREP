package com.algoprep.problembank.application.ports.in;

import com.algoprep.problembank.application.dto.StudentClassViews.MyAssignmentGroups;
import com.algoprep.problembank.application.dto.StudentClassViews.MyClassProblems;
import java.util.UUID;

/** The learner's class assignments (DD F2 sections 5 and 5A). */
public interface MyClassAssignmentsUseCase {
  /** Section 5: the three most recently assigned groups, published problems only. */
  MyAssignmentGroups groups(UUID userId);

  /**
   * Section 5A: every class or one class, withdrawn problems included, one page at a time. {@code
   * classId}, {@code page} and {@code pageSize} are raw; bad paging values fall back to defaults.
   */
  MyClassProblems problems(UUID userId, String classId, String page, String pageSize);
}
