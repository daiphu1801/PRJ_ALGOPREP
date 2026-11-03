package com.algoprep.problembank.application.dto;

/** F2-owned configuration values, with original units and no language multipliers. */
public record JudgeGradingDefaults(
    int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, int compileTimeoutSeconds) {}
