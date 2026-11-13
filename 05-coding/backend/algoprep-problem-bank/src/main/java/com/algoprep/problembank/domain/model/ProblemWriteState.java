package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Locked, non-deleted problem metadata used for authoring authorization and optimistic locking. */
public record ProblemWriteState(UUID authorId, String status, Instant updatedAt) {}
