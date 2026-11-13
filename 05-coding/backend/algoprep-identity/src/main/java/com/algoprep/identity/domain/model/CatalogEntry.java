package com.algoprep.identity.domain.model;

import java.util.UUID;

/** A seeded function or action; {@code name} is null for an action. */
public record CatalogEntry(UUID id, String code, String name) {}
