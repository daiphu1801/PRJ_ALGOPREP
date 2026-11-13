package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** An active student of a class with the contact data an instructor may read. */
public record ClassRosterEntry(
    UUID studentId, String displayName, String email, String phone, Instant joinedAt) {}
