package com.algoprep.identity.application.dto;

import java.util.UUID;

/**
 * Toggle of one cell; {@code granted} is boxed so a missing value is reported, not read as false.
 */
public record UpdateRolePermissionCommand(
    UUID actorId, UUID roleId, UUID functionId, UUID actionId, Boolean granted) {}
