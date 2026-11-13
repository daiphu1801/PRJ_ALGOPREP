package com.algoprep.judge.domain.model;

import java.math.BigDecimal;

public record WorkerSettings(
    BigDecimal timeMultiplier,
    BigDecimal memoryMultiplier,
    int defaultTimeMs,
    int defaultMemoryMb,
    int defaultOutputKb,
    int compileSeconds,
    boolean networkEnabled,
    boolean processLimitEnabled,
    boolean returnStderr) {}
