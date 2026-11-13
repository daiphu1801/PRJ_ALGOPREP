package com.algoprep.judge.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** List projection deliberately excludes source, diagnostics and testcase content. */
public record ScopedSubmission(
    UUID id,
    UUID classId,
    UUID studentId,
    UUID problemId,
    String language,
    String status,
    Integer passed,
    Integer total,
    BigDecimal score,
    Instant submittedAt) {}
