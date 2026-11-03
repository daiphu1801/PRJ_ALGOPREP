package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One account as the admin user list shows it (#35). {@code solvedCount} and {@code
 * totalSubmissions} of the BD are left out until F4 fills their read model.
 *
 * @param lastActiveAt latest recorded activity ({@code users.last_active_at}, set at login at most
 *     once a minute), {@code null} when the account never logged in
 * @param hasPassword {@code password_hash IS NOT NULL}
 * @param anonymized {@code anonymized_at IS NOT NULL}; the unlock button is disabled for these
 */
public record AdminUserItem(
    UUID id,
    String displayName,
    String email,
    UUID roleId,
    String roleName,
    String baseCategory,
    String status,
    Instant lastActiveAt,
    boolean hasPassword,
    boolean anonymized) {}
