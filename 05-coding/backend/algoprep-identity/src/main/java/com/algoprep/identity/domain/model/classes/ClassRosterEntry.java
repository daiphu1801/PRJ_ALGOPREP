package com.algoprep.identity.domain.model.classes;

import java.time.Instant;
import java.util.UUID;

/**
 * An active student of a class with the contact data an instructor may read. {@code avatarVersion}
 * is null when the student has no avatar (DD api F1 section 0.9.5). {@code accountInactive} is true
 * when the account is no longer active (deleted by its owner or locked by an admin); the student
 * still counts in the class, the flag only lets the screen say so
 * (DEC-2026-1126-roster-inactive-account-label).
 */
public record ClassRosterEntry(
    UUID studentId,
    String displayName,
    String email,
    String phone,
    Instant joinedAt,
    Long avatarVersion,
    boolean accountInactive) {}
