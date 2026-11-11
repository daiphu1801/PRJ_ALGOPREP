package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.FeatureCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/** Response of endpoints 18 to 20 (DD api F5 section 18.3). */
public record BudgetStatus(
    long limitTokens,
    long usedTokens,
    LocalDate periodStart,
    Instant lockedAt,
    boolean locked,
    LocalDate projectedRunOutDate,
    Map<FeatureCode, Long> featureBreakdown,
    int warnPercent) {}
