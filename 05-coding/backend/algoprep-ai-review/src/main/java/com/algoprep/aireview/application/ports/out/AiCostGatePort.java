package com.algoprep.aireview.application.ports.out;

import com.algoprep.aireview.domain.model.CostReservation;
import com.algoprep.aireview.domain.model.FeatureCode;
import java.util.UUID;

/** Cooldown plus hourly quota in Redis (BR-AI-04 steps 3, 4, 8, 9). */
public interface AiCostGatePort {
  /**
   * One atomic step: set the cooldown key to a fresh request id, then count the hourly quota. Redis
   * unavailable returns a {@code BYPASSED} reservation (fail-open, WARN logged).
   */
  CostReservation reserve(
      UUID userId, FeatureCode featureCode, int hourlyLimit, int cooldownSeconds);

  /** Undoes a {@code GRANTED} reservation exactly once; a second call is a no-op. */
  void refund(CostReservation reservation);
}
