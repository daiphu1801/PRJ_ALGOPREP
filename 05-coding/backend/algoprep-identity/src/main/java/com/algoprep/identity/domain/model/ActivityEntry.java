package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One entry of the "recent activity" feed; {@code classId} is null for an entry with no class. */
public record ActivityEntry(
    UUID id,
    UUID actorUserId,
    UUID classId,
    String activityType,
    String refType,
    UUID refId,
    Instant occurredAt) {}
