package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A class as its owning instructor sees it; {@code studentCount} counts active enrolments. */
public record InstructorClass(
    UUID id,
    String name,
    String description,
    ScheduleSpec schedule,
    int studentCount,
    Instant createdAt) {}
