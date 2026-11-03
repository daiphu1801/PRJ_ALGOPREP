package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.AuditLogCursor;
import java.util.UUID;

/**
 * One read of the live stream (#66). Exactly one of the starting points applies, in this order:
 * {@code after}, then {@code lastEventId}, then neither (answers the newest position only).
 *
 * @param after read rows strictly newer than this position
 * @param lastEventId the {@code Last-Event-ID} of a reconnecting client
 * @param upTo stop at this position, inclusive; null means no bound
 * @param limit maximum rows
 */
public record AuditStreamRequest(
    AuditLogCursor after, UUID lastEventId, AuditLogCursor upTo, int limit) {}
