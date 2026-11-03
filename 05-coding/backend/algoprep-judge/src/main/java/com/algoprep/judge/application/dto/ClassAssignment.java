package com.algoprep.judge.application.dto;

import java.time.Instant;
import java.util.UUID;

/** History interval [assignedAt, removedAt), with a null end for a current assignment. */
public record ClassAssignment(
    UUID classId, UUID problemId, String problemTitle, Instant assignedAt, Instant removedAt) {}
