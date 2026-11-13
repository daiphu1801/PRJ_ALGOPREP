package com.algoprep.aireview.domain.model;

import java.util.UUID;

/** Usage of one problem (rows whose {@code problem_id} is not null). */
public record ProblemUsage(UUID problemId, long tokens, int calls) {}
