package com.algoprep.identity.domain.model;

import java.util.UUID;

/** A role row as the matrix endpoints see it. */
public record RbacRole(UUID id, String code, String name, String baseCategory, boolean isSystem) {}
