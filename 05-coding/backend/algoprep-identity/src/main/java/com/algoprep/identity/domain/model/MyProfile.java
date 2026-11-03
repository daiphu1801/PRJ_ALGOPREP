package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Read model of {@code GET /me/profile} (DD api F1 section 0.2.1). Carries no hash and no provider
 * account details; {@code hasPassword} is derived from {@code password_hash IS NOT NULL}.
 */
public record MyProfile(
    UUID userId,
    String displayName,
    String email,
    String schoolOrCompany,
    String currentPosition,
    String targetPosition,
    String defaultLanguage,
    String phone,
    boolean hasPassword,
    Instant passwordChangedAt,
    List<String> linkedProviders,
    Instant joinedAt,
    String roleName,
    boolean mustChangePassword) {}
