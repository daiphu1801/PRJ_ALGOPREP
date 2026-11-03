package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One stored audit row joined with its actor, exactly as read; nothing is masked yet (DD BR-16
 * applies in the mapping layer, never in SQL).
 *
 * @param id row id
 * @param createdAt when the action was recorded
 * @param actorDisplayName display name of the actor
 * @param actorEmail e-mail of the actor, possibly the anonymised placeholder
 * @param actionType stable action code
 * @param targetType kind of the affected object
 * @param targetId id of the affected object, may be null
 * @param beforeJson snapshot before the change as JSON text, may be null
 * @param afterJson snapshot after the change as JSON text, may be null
 */
public record AuditLogRow(
    UUID id,
    Instant createdAt,
    String actorDisplayName,
    String actorEmail,
    String actionType,
    String targetType,
    String targetId,
    String beforeJson,
    String afterJson) {}
