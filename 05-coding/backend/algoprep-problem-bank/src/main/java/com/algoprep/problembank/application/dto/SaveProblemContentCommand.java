package com.algoprep.problembank.application.dto;

import com.algoprep.problembank.domain.model.ProblemContentData;
import java.time.Instant;
import java.util.UUID;

/**
 * Trusted internal caller supplies identity and the granted CREATE/UPDATE cell; null id creates a
 * draft.
 */
public record SaveProblemContentCommand(
    UUID problemId,
    UUID actorId,
    boolean admin,
    boolean actionGranted,
    Instant expectedUpdatedAt,
    ProblemContentData content) {}
