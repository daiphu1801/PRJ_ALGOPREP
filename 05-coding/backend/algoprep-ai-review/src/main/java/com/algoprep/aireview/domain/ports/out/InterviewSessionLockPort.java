package com.algoprep.aireview.domain.ports.out;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Redis lock on {@code ai:session:<sessionId>}: one turn or hint per session (BR-AI-07). */
public interface InterviewSessionLockPort {
  /** Empty, without running {@code work}, when another caller already holds the lock. */
  <T> Optional<T> withLock(UUID sessionId, Supplier<T> work);
}
