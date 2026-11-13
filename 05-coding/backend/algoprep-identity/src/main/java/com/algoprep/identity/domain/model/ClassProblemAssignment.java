package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One {@code ProblemAssignedToClass} event (DD api F2 section 53). {@code occurredAt} is the moment
 * the problem was assigned to the class.
 */
public record ClassProblemAssignment(
    UUID eventId, UUID classId, UUID problemId, UUID actorUserId, Instant occurredAt) {}
