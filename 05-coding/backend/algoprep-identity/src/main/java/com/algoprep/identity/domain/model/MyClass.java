package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One class the caller currently belongs to (DD api F1 section 0.4.1). {@code schedule} is the
 * parsed JSONB object (or null), {@code description} is empty when the column is NULL.
 */
public record MyClass(
    UUID classId,
    String name,
    String description,
    Object schedule,
    String instructorName,
    Instant joinedAt) {}
