package com.algoprep.identity.application.ports.out;

import java.util.UUID;

/** At most 10 avatar changes or removals per user per hour (DD logic F1 BR-25, BR-26). */
public interface AvatarRateLimitPort {

  /** Counts one change; false when this one is over the limit. Fails open when Redis is down. */
  boolean tryAcquire(UUID userId);
}
