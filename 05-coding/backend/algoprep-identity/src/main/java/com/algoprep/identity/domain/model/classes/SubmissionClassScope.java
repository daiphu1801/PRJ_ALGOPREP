package com.algoprep.identity.domain.model.classes;

import java.time.Instant;
import java.util.UUID;

/**
 * Historical membership; an empty class has a null student and membership timestamps. {@code
 * avatarVersion} is null for a student without avatar (DD api F1 section 0.9.5).
 */
public record SubmissionClassScope(
    UUID classId,
    String className,
    UUID studentId,
    String studentName,
    Instant joinedAt,
    Instant leftAt,
    Long avatarVersion) {}
