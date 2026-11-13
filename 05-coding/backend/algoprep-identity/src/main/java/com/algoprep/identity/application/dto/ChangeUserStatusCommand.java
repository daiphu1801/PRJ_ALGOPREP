package com.algoprep.identity.application.dto;

import java.util.UUID;

/**
 * Lock or unlock of #37; every value is read as sent so a wrong type is a rule violation, not a
 * Jackson error.
 */
public record ChangeUserStatusCommand(
    UUID actorId,
    UUID userId,
    Object targetStatus,
    Object reason,
    Object notifyByEmail,
    Object acknowledgeSelfImpact) {}
