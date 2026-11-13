package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Role change of #36; the target role and the self-impact flag are read as sent. */
public record ChangeUserRoleCommand(
    UUID actorId, UUID userId, UUID targetRoleId, Object acknowledgeSelfImpact) {}
