package com.algoprep.problembank.application.dto;

import java.time.Instant;
import java.util.UUID;

/** Status change of DD F2 section 40; {@code status} is UNPUBLISHED or PUBLISHED. */
public record UpdateProblemStatusCommand(
    UUID problemId, UUID actorId, boolean admin, String status, Instant expectedUpdatedAt) {}
