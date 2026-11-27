package com.algoprep.aireview.application.ports.out;

import java.util.UUID;

/**
 * Per-session count of sweep scoring attempts that failed because of the LLM (JOB-AI-04). Kept in
 * Redis, not in a column; losing it restarts the count, which the DD accepts.
 */
public interface InterviewSweepRetryPort {
  /** Adds one failure and returns the new count; 1 when the store is unavailable. */
  int recordFailure(UUID sessionId);

  void clear(UUID sessionId);
}
