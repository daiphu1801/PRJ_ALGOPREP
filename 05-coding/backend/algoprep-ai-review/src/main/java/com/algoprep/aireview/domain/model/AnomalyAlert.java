package com.algoprep.aireview.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One OPEN row of {@code ai_usage_anomaly_alerts}: a soft alert that never locks an account. */
public record AnomalyAlert(
    UUID id,
    UUID userId,
    Instant windowStart,
    Instant windowEnd,
    int callCount,
    BigDecimal baselineAvg,
    BigDecimal ratio,
    Instant createdAt) {}
