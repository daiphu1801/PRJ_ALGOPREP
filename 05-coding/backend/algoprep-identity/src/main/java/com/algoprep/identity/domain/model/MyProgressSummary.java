package com.algoprep.identity.domain.model;

/**
 * Counters of #47. {@code acceptanceRatePercent} is null when no submission is eligible, and {@code
 * topLanguage} is null when none was counted.
 */
public record MyProgressSummary(
    int solvedProblemCount,
    int totalSubmissions,
    int acceptedCount,
    Integer acceptanceRatePercent,
    int firstTryAcceptedCount,
    String topLanguage) {}
