package com.algoprep.aireview.domain.model;

import java.util.UUID;

/** Title and level of one problem, as problem-bank answers it (DD F5 section 1.3). */
public record ProblemSummary(
    UUID problemId, String title, String levelCode, String levelDisplayName) {}
