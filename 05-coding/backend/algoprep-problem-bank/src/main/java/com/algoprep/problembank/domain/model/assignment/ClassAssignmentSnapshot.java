package com.algoprep.problembank.domain.model;

import java.time.Instant;
import java.util.UUID;

/** One assignment interval, including removed and repeated assignments of the same problem. */
public record ClassAssignmentSnapshot(
    UUID classId, UUID problemId, String problemTitle, Instant assignedAt, Instant removedAt) {}
