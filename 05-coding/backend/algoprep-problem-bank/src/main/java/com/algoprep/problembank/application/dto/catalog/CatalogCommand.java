package com.algoprep.problembank.application.dto;

import java.util.List;
import java.util.UUID;

/** Actor information is supplied by the trusted transport, never deserialized from a request. */
public record CatalogCommand(
    UUID actorId, boolean admin, UUID id, String name, List<UUID> orderedIds) {}
