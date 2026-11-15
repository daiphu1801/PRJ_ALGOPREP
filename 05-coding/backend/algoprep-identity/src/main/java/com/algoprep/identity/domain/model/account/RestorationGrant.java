package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A ticket is bound to a particular self-deletion, so it cannot restore a later deletion. */
public record RestorationGrant(UUID userId, Instant deactivatedAt, Instant expiresAt) {}
