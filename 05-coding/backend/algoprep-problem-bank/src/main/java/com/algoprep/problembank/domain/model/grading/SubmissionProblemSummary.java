package com.algoprep.problembank.domain.model.grading;

import java.util.UUID;

public record SubmissionProblemSummary(
    UUID problemId,
    String code,
    String title,
    String slug,
    String levelDisplayName,
    boolean withdrawn) {}
