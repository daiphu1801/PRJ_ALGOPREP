package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One {@code SubmissionAccepted} event (DD api F4 section 25.3). It carries no class: F1 works the
 * class out itself. {@code occurredAt} is when the submission reached its final state.
 */
public record AcceptedSubmission(
    UUID eventId, UUID userId, UUID problemId, UUID submissionId, Instant occurredAt) {}
