package com.algoprep.identity.domain.model;

import java.util.UUID;

/** A class reached through an invite code: just enough to answer #25. */
public record ClassRef(UUID id, String name) {}
