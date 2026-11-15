package com.algoprep.problembank.application.dto;

import java.util.UUID;

/** Internal publication result, without hidden content or object references. */
public record SnapshotPublicationResult(
    UUID problemId, int version, int testcaseCount, boolean created, boolean versionBumped) {}
