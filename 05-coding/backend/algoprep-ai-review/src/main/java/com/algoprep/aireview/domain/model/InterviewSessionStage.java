package com.algoprep.aireview.domain.model;

/** Column {@code interview_sessions.stage}: three running stages plus two terminal ones. */
public enum InterviewSessionStage {
  EXPLAIN,
  CHALLENGE,
  SCALE_UP,
  COMPLETED,
  ABANDONED;

  public boolean running() {
    return this == EXPLAIN || this == CHALLENGE || this == SCALE_UP;
  }

  /** The running stage as the prompt-side enum; only valid while {@link #running()}. */
  public InterviewStage toRunning() {
    return InterviewStage.valueOf(name());
  }
}
