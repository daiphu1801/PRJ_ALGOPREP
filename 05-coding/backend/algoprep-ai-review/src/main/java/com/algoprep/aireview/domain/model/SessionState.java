package com.algoprep.aireview.domain.model;

import java.util.Objects;

/**
 * Server-computed state printed into the system message (BR-AI-01 step 3). Holds enums, numbers and
 * booleans only: there is no String field, so no user text can reach the system message through it.
 */
public record SessionState(
    InterviewStage stage,
    int stageTurns,
    int stageTurnCeiling,
    int turnCount,
    Integer maxTurns,
    InterviewerLevel interviewerLevel,
    boolean hintAllowed,
    boolean forcedStageChange) {
  public SessionState {
    Objects.requireNonNull(stage, "stage");
  }
}
