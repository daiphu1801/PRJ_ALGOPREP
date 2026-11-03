package com.algoprep.identity.application.ports.out;

import java.util.UUID;

/** One export of each kind per user every five minutes (DD api F1 #16, validation section 2). */
public interface ExportRateLimitPort {

  /**
   * Takes the slot for {@code scope} if it is free. Dependency errors propagate: the limit never
   * fails open.
   *
   * @return false when the same kind was exported less than five minutes ago
   */
  boolean tryAcquire(UUID userId, String scope);
}
