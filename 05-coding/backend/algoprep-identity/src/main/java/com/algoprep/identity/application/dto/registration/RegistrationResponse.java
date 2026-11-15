package com.algoprep.identity.application.dto.registration;

import java.util.UUID;

/** Public registration result. */
public record RegistrationResponse(UUID userId, String email) {}
