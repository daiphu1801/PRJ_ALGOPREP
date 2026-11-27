package com.algoprep.identity.domain.model.auth;

import java.util.UUID;

/** A session family was explicitly revoked; transports must disconnect that family. */
public record SessionRevoked(UUID familyId) {}
