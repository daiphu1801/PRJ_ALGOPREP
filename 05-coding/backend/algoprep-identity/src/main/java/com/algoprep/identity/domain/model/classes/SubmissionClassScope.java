package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Historical membership; an empty class has a null student and membership timestamps. */
public record SubmissionClassScope(
    UUID classId,
    String className,
    UUID studentId,
    String studentName,
    Instant joinedAt,
    Instant leftAt) {}
