package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Read models of the learner's class assignment screens (DD F2 sections 5 and 5A). */
public final class StudentClassModels {
  private StudentClassModels() {}

  /** What the judge module reports for one assignment interval of the caller. */
  public enum ClassSolveState {
    SOLVED,
    ATTEMPTED,
    UNSOLVED
  }

  /** One active (class, problem) pair; {@code withdrawn} is true for an unpublished problem. */
  public record MyAssignment(
      UUID classId,
      UUID problemId,
      String code,
      String title,
      String levelDisplayName,
      Instant assignedAt,
      boolean withdrawn) {}

  /** Identifies one assignment interval: a re-assigned problem has one key per assignment. */
  public record AssignmentKey(UUID classId, UUID problemId, Instant assignedAt) {}
}
