package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One {@code SolutionReviewCompleted} event (DD api F1 section 2, DD api F5 section 33). Only the
 * fields the feed needs are read; {@code occurredAt} is the moment the review was stored.
 */
public record CompletedSolutionReview(
    UUID eventId, UUID userId, UUID reviewId, Instant occurredAt) {}
