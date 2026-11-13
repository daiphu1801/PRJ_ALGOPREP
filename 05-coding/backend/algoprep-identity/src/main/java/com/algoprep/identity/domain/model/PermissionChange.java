package com.algoprep.identity.domain.model;

import java.util.UUID;

/** Outcome of one cell toggle; {@code changed} is false when the cell already had the value. */
public record PermissionChange(
    UUID roleId, UUID functionId, UUID actionId, boolean granted, boolean changed) {}
