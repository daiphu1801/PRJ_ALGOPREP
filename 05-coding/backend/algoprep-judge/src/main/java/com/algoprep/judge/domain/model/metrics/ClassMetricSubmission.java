package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Minimal metrics projection; never carries learner source or testcase data. */
public record ClassMetricSubmission(
    UUID id,
    UUID studentId,
    UUID problemId,
    String verdict,
    BigDecimal scoreRatio,
    Instant submittedAt) {}
