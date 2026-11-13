package com.algoprep.aireview.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Result of marking an alert reviewed; {@code alreadyReviewed} keeps the call idempotent. */
public record AlertReview(UUID alertId, Instant reviewedAt, boolean alreadyReviewed) {}
