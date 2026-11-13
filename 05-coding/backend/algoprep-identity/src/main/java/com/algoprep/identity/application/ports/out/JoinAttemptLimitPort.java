package com.algoprep.identity.application.ports.out;

import java.util.UUID;

/**
 * Sliding window of wrong invite-code attempts: five within 15 minutes block the user (DD api F1
 * #25). Infrastructure errors must not count as an attempt, so implementations fail open.
 */
public interface JoinAttemptLimitPort {

  /** True when at least five wrong attempts fall inside the window. */
  boolean isBlocked(UUID userId);

  /** Adds one wrong attempt to the window. */
  void recordFailure(UUID userId);

  /** Forgets the window after a successful join. */
  void clear(UUID userId);
}
