package com.algoprep.problembank.application.dto;

import java.time.Instant;
import java.util.UUID;
import tools.jackson.databind.JsonNode;

/** Caller identity comes from the authenticated principal, never from the request body. */
public record SaveProblemSpecCommand(
    UUID problemId, UUID actorId, boolean admin, Instant expectedUpdatedAt, JsonNode body) {}
