package com.algoprep.judge.domain.model;

import java.util.UUID;

/** Caller-owned audit contract; the bridge joins the caller's transaction. */
public record JudgeAuditEntry(
    UUID actorUserId,
    String actionType,
    String targetType,
    String targetId,
    String beforeJson,
    String afterJson) {}
