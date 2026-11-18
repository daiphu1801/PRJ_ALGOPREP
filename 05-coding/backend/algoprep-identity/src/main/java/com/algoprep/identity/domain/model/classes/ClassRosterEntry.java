package com.algoprep.identity.domain.model.classes;

import java.time.Instant;
import java.util.UUID;

/**
 * An active student of a class with the contact data an instructor may read. {@code avatarVersion}
 * is null when the student has no avatar (DD api F1 section 0.9.5).
 */
public record ClassRosterEntry(
    UUID studentId,
    String displayName,
    String email,
    String phone,
    Instant joinedAt,
    Long avatarVersion) {}
