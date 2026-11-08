package com.algoprep.harness.application.ports.out;

import java.util.UUID;

/** P6 throttle (DD F3 3.7): at most 10 previews per second per user. */
public interface PreviewRateLimitPort {
  /** Returns 0 when the call may proceed, otherwise the whole seconds the caller should wait. */
  long acquire(UUID userId);
}
