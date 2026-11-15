package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One invite code of a class; many can be valid at the same time. */
public record ClassInviteCode(UUID id, String code, Instant expiresAt) {}
