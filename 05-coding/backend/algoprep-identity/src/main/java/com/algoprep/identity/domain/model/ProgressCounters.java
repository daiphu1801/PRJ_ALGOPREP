package com.algoprep.identity.domain.model;

/** Raw stored counters behind #47; the rate is derived by the handler, not stored. */
public record ProgressCounters(
    int solvedProblemCount,
    int totalSubmissions,
    int acEligibleCount,
    int acceptedCount,
    int firstTryAcceptedCount,
    String topLanguage) {}
