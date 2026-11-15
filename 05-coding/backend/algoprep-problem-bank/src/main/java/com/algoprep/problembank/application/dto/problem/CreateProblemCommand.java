package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.domain.model.ProblemContentData;
import java.util.UUID;

/**
 * Create of DD F2 section 36: any limit left null is filled from the grading defaults; {@code
 * content.limits()} is ignored and replaced by the merged value. {@code duplicatedFromProblemId}
 * names the source whose spec and testcases are copied (BR-10); null for an ordinary create.
 */
public record CreateProblemCommand(
    UUID actorId,
    boolean admin,
    ProblemContentData content,
    PartialLimits limits,
    UUID duplicatedFromProblemId) {

  /** An ordinary create, no source to copy from. */
  public CreateProblemCommand(
      UUID actorId, boolean admin, ProblemContentData content, PartialLimits limits) {
    this(actorId, admin, content, limits, null);
  }

  /** The limits the author typed; null means "use the system default". */
  public record PartialLimits(
      Integer timeLimitMs,
      Integer memoryLimitMb,
      Integer maxOutputSizeKb,
      Integer maxSubmissionsPerHour) {}
}
