package com.algoprep.judge.domain.model;

/** Current base limits, before any language multipliers; units remain explicit. */
public record GradingDefaults(
    int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, int compileTimeoutSeconds) {}
