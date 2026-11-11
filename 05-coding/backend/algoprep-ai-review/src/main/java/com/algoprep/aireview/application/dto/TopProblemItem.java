package com.algoprep.aireview.application.dto;

import java.util.UUID;

/** Endpoint 31 row; the three problem-bank fields are null when that module is unavailable. */
public record TopProblemItem(
    UUID problemId,
    String problemTitle,
    String levelCode,
    String levelDisplayName,
    long tokens,
    int calls,
    int avgTokensPerCall) {}
