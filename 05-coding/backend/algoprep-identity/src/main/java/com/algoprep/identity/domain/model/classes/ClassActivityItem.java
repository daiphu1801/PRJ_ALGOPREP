package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One entry of the instructor class feed (#45); the actor name is read from {@code users}. */
public record ClassActivityItem(
    UUID id,
    String activityType,
    UUID actorUserId,
    String actorDisplayName,
    UUID scopeClassId,
    String refType,
    UUID refId,
    Instant occurredAt) {}
