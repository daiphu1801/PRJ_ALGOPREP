package com.algoprep.identity.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One {@code SubmissionGraded} event (DD api F4 section 25.2) as F1 reads it. {@code ratio} is null
 * for {@code COMPILE_ERROR} and {@code SYSTEM_ERROR}; {@code submittedAt} is the time of the
 * submission, not of the grading.
 */
public record GradedSubmission(
    UUID eventId,
    UUID userId,
    UUID problemId,
    String verdict,
    BigDecimal ratio,
    String language,
    Instant submittedAt,
    boolean firstAttemptForProblem) {}
