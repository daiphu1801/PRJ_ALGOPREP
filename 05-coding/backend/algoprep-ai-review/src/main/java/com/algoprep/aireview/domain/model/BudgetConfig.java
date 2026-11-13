package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.time.LocalDate;

/** Row {@code id = 1} of {@code ai_token_budget_configs}. {@code limitTokens = 0} means unset. */
public record BudgetConfig(
    long limitTokens,
    LocalDate periodStart,
    Instant lockedAt,
    int reviewPerHour,
    int interviewPerHour,
    int cooldownSeconds) {}
