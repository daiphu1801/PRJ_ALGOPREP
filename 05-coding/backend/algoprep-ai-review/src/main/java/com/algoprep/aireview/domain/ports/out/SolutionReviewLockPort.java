package com.algoprep.aireview.domain.ports.out;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Redis lock on {@code ai:review:<submissionId>} held across the LLM call (BR-AI-02 step 4). */
public interface SolutionReviewLockPort {
  /**
   * Runs {@code work} while holding the lock and releases it afterwards. Returns empty, without
   * running {@code work}, when another caller already holds the lock.
   */
  <T> Optional<T> withLock(UUID submissionId, Supplier<T> work);
}
