package com.algoprep.identity.application.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * One audit row as the API shows it (DD 0.7.2 {@code AuditLogItemDto}). {@code safeParams} is
 * already masked by BR-16; raw {@code before_json} and {@code after_json} never leave the server.
 *
 * @param id row id, also the SSE event id
 * @param createdAt when the action was recorded
 * @param actorDisplayName display name of the actor
 * @param actorEmail e-mail of the actor, possibly the anonymised placeholder
 * @param actionType stable action code; the client builds the sentence from it
 * @param categoryCode {@code AUTH}, {@code PERMISSION}, {@code CONFIG} or {@code CONTENT}
 * @param serviceCode lower-case module name
 * @param targetType kind of the affected object
 * @param targetId id of the affected object, may be null
 * @param safeParams masked parameters for the sentence template
 */
public record AuditLogItem(
    UUID id,
    Instant createdAt,
    String actorDisplayName,
    String actorEmail,
    String actionType,
    String categoryCode,
    String serviceCode,
    String targetType,
    String targetId,
    Map<String, Object> safeParams) {}
