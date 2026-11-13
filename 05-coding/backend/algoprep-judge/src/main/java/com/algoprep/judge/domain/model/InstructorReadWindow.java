package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.UUID;

public record InstructorReadWindow(
    UUID classId,
    UUID studentId,
    UUID problemId,
    Instant joinedAt,
    Instant leftAt,
    Instant assignedAt,
    Instant removedAt) {}
