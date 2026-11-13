package com.algoprep.identity.application.dto;

import java.util.UUID;

/**
 * Partial update: a {@code null} field was not sent and keeps its stored value; an empty string
 * clears an optional field. {@code email}, role and status are not part of this command.
 */
public record UpdateMyProfileCommand(
    UUID userId,
    String displayName,
    String schoolOrCompany,
    String currentPosition,
    String targetPosition,
    String phone) {}
