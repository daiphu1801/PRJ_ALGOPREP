package com.algoprep.problembank.domain.model;

import java.util.UUID;

public record CatalogAuditEntry(
    UUID actorUserId,
    String actionType,
    String targetType,
    String targetId,
    String beforeJson,
    String afterJson) {}
