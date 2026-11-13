package com.algoprep.aireview.domain.model;

import java.util.UUID;

/**
 * What one {@code reserve} call did in Redis. {@code retryAfterSeconds} is only set for the two
 * rejections. A refund needs {@code requestId}, {@code epochHour} and {@code quotaIncremented}.
 */
public record CostReservation(
    GateOutcome outcome,
    UUID userId,
    FeatureCode featureCode,
    String requestId,
    long epochHour,
    boolean quotaIncremented,
    long retryAfterSeconds) {
  public boolean granted() {
    return outcome == GateOutcome.GRANTED;
  }

  public boolean rejected() {
    return outcome == GateOutcome.COOLDOWN || outcome == GateOutcome.QUOTA_EXCEEDED;
  }
}
