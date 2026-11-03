package com.algoprep.identity.domain.model;

import java.util.UUID;

/**
 * The part of an account that authentication needs.
 *
 * @param id user id
 * @param passwordHash BCrypt hash, {@code null} for an account that has never had a password
 * @param displayName name shown in the UI
 * @param status lifecycle state
 * @param mustChangePassword true for an Admin-created account until its first password change
 * @param roleCode code of the user's role
 * @param baseCategory layer-1 category of that role
 */
public record UserAccount(
    UUID id,
    String passwordHash,
    String displayName,
    UserStatus status,
    boolean mustChangePassword,
    String roleCode,
    BaseCategory baseCategory) {}
