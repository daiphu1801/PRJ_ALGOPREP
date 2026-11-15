package com.algoprep.problembank.domain.model;

import com.algoprep.problembank.domain.model.problem.ProblemContentData;
import java.time.Instant;
import java.util.UUID;

/** Internal content result, deliberately distinct from the full HTTP ProblemAuthoringDetailDto. */
public record ProblemContentSnapshot(
    UUID id,
    String code,
    UUID authorId,
    String status,
    ProblemContentData content,
    Instant updatedAt) {}
