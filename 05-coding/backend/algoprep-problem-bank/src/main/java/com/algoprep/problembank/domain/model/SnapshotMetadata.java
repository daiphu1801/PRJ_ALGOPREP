package com.algoprep.problembank.domain.model;

import java.util.UUID;

/** Database reference to an immutable object, never exported outside trusted readers. */
public record SnapshotMetadata(
    UUID problemId, int version, String objectKey, int testcaseCount, String contentSha256) {}
