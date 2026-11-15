package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One {@code MockInterviewFinished} event (DD api F1 section 2, DD api F5 section 33). Only the
 * fields the feed needs are read; the score and the rubric in the payload are not stored here.
 */
public record FinishedMockInterview(
    UUID eventId, UUID userId, UUID sessionId, Instant occurredAt) {}
