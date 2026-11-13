package com.algoprep.identity.domain.model;

import java.util.UUID;

/**
 * One audit row to append (DD BR-24).
 *
 * @param actorUserId who acted, never null
 * @param actionType stable action code
 * @param targetType kind of the affected object
 * @param targetId id of the affected object, may be null
 * @param beforeJson snapshot before the change, may be null
 * @param afterJson snapshot after the change, may be null
 */
public record AuditEntry(
    UUID actorUserId,
    String actionType,
    String targetType,
    String targetId,
    String beforeJson,
    String afterJson) {}
