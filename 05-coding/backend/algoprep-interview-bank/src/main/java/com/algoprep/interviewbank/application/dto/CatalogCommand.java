package com.algoprep.interviewbank.application.dto;

import java.util.List;
import java.util.UUID;

/**
 * Actor facts come from the trusted transport, never from the request body. {@code displayName} and
 * {@code usesStarFramework} are null when absent from a PATCH.
 */
public record CatalogCommand(
    UUID actorId,
    boolean admin,
    UUID id,
    String displayName,
    Boolean usesStarFramework,
    List<UUID> orderedIds) {}
