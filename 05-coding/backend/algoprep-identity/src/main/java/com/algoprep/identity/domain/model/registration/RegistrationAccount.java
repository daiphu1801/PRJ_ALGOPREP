package com.algoprep.identity.domain.model;

import java.util.UUID;

/** New self-registration; role and initial state are fixed by the repository contract. */
public record RegistrationAccount(UUID id, String email, String passwordHash, String displayName) {}
