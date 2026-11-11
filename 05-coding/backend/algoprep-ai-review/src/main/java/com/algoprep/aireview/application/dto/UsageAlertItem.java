package com.algoprep.aireview.application.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Endpoint 28: a stored ANOMALY alert or the derived BUDGET alert (fields of the other null). */
public record UsageAlertItem(
    Type alertType,
    UUID alertId,
    String status,
    Integer callCount,
    BigDecimal baselineAvg,
    BigDecimal ratio,
    Instant windowStart,
    Instant windowEnd,
    UUID userId,
    String displayName,
    LocalDate projectedRunOutDate,
    BigDecimal usedPercent) {
  /** Kind of alert. */
  public enum Type {
    BUDGET,
    ANOMALY
  }
}
