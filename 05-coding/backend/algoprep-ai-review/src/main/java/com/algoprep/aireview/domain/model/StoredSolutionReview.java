package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One {@code solution_reviews} row; {@code resultJson} is the normalised JSON text. */
public record StoredSolutionReview(
    UUID id,
    UUID submissionId,
    UUID userId,
    UUID problemId,
    String language,
    String sourceHash,
    UUID promptTemplateId,
    String resultJson,
    int readabilityScore,
    boolean servedFromCache,
    Instant createdAt) {}
