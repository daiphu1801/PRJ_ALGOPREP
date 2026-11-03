package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Public registration result. */
public record RegistrationResponse(UUID userId, String email) {}
