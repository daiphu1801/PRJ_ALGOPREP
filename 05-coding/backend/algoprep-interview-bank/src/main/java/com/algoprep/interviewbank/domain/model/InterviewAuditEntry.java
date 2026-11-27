package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/** Caller-owned audit contract; the bootstrap bridge translates it for identity. */
public record InterviewAuditEntry(
    UUID actorUserId,
    String actionType,
    String targetType,
    String targetId,
    String beforeJson,
    String afterJson) {}
