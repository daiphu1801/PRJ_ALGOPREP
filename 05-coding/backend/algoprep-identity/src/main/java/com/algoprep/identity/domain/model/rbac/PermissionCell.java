package com.algoprep.identity.domain.model;

import java.util.UUID;

/** One role, function, action triple; a missing stored row reads as not granted. */
public record PermissionCell(UUID roleId, UUID functionId, UUID actionId, boolean granted) {}
